/**
 * AuthViewModel.kt - Authentication State Management
 * 
 * This ViewModel handles all authentication-related operations and state management.
 * It serves as the bridge between UI components and the authentication repository,
 * managing user sign-in, sign-up, sign-out operations and authentication state.
 * 
 * Key Responsibilities:
 * - User authentication operations (sign in, sign up, sign out)
 * - Authentication state management and UI state updates
 * - Error handling for authentication failures
 * - Loading state management during auth operations
 * - Reactive authentication state monitoring
 * 
 * Architecture:
 * - Follows MVVM pattern with reactive state flows
 * - Integrates with Repository pattern for authentication
 * - Provides reactive UI state updates
 * - Handles coroutine lifecycle management for async operations
 */
package com.example.pageapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pageapp.data.User
import com.example.pageapp.data.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * AuthUiState - Represents the complete authentication UI state
 * 
 * This data class encapsulates all authentication-related state information
 * needed by UI components to render login/registration screens and handle user interactions.
 * 
 * @property user Current authenticated user object (null if not logged in)
 * @property isLoading Boolean indicating if authentication operations are in progress
 * @property error Error message string for displaying authentication failures
 * @property isLoggedIn Boolean indicating current authentication status
 */
data class AuthUiState(
    // Current authenticated user (null when not logged in)
    val user: User? = null,
    // Loading state for showing progress indicators during auth operations
    val isLoading: Boolean = false,
    // Error message for displaying when authentication operations fail
    val error: String? = null,
    // Authentication status flag for UI conditional rendering
    val isLoggedIn: Boolean = false
)

/**
 * AuthViewModel - Authentication business logic controller
 * 
 * This ViewModel manages authentication operations and provides reactive state
 * updates to UI components. It handles all authentication workflows including
 * sign-in, sign-up, sign-out, and automatic authentication state monitoring.
 * 
 * @param authRepository Repository for authentication operations
 */
class AuthViewModel(
    // Repository for handling authentication operations with Firebase
    private val authRepository: AuthRepository
) : ViewModel() {

    // Private mutable state flow for internal authentication state updates
    private val _uiState = MutableStateFlow(AuthUiState())
    // Public read-only state flow for UI observation
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * ViewModel initialization
     * Sets up automatic authentication state monitoring
     */
    init {
        // Launch coroutine to observe authentication state changes
        viewModelScope.launch {
            // Collect authentication state changes from repository
            // This automatically updates UI state when user logs in/out
            authRepository.currentUser.collect { user ->
                // Update UI state with current user and login status
                _uiState.update { 
                    it.copy(
                        user = user,                    // Current user object
                        isLoggedIn = user != null       // Set login status based on user existence
                    )
                }
            }
        }
    }

    /**
     * Sign in existing user with email and password
     * 
     * Handles user authentication with loading states and error handling.
     * Updates UI state based on authentication result.
     * 
     * @param email User's email address
     * @param password User's password
     */
    fun signIn(email: String, password: String) {
        // Launch coroutine for authentication operation
        viewModelScope.launch {
            // Set loading state and clear any previous errors
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Attempt authentication through repository
            authRepository.signIn(email, password)
                .onSuccess { user ->
                    // Authentication successful - update UI state with user data
                    _uiState.update { 
                        it.copy(
                            isLoading = false,     // Clear loading state
                            user = user,           // Set authenticated user
                            isLoggedIn = true      // Set login status
                        )
                    }
                }
                .onFailure { exception ->
                    // Authentication failed - update UI state with error
                    _uiState.update { 
                        it.copy(
                            isLoading = false,        // Clear loading state
                            error = exception.message // Set error message for UI display
                        )
                    }
                }
        }
    }

    /**
     * Sign up new user with email, password, and display name
     * 
     * Handles user registration with loading states and error handling.
     * Updates UI state based on registration result.
     * 
     * @param email New user's email address
     * @param password New user's chosen password
     * @param displayName New user's display name
     */
    fun signUp(email: String, password: String, displayName: String) {
        // Launch coroutine for registration operation
        viewModelScope.launch {
            // Set loading state and clear any previous errors
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Attempt user registration through repository
            authRepository.signUp(email, password, displayName)
                .onSuccess { user ->
                    // Registration successful - update UI state with new user data
                    _uiState.update { 
                        it.copy(
                            isLoading = false,     // Clear loading state
                            user = user,           // Set newly registered user
                            isLoggedIn = true      // Set login status
                        )
                    }
                }
                .onFailure { exception ->
                    // Registration failed - update UI state with error
                    _uiState.update { 
                        it.copy(
                            isLoading = false,        // Clear loading state
                            error = exception.message // Set error message for UI display
                        )
                    }
                }
        }
    }

    /**
     * Sign out current user
     * 
     * Clears authentication state and updates UI accordingly.
     * Delegates actual sign-out to repository.
     */
    fun signOut() {
        // Perform sign-out through repository (clears Firebase auth state)
        authRepository.signOut()
        
        // Update UI state to reflect signed-out status
        _uiState.update { 
            it.copy(
                user = null,           // Clear user data
                isLoggedIn = false,    // Set logged out status
                error = null           // Clear any existing errors
            )
        }
    }

    /**
     * Clear authentication error messages
     * 
     * Allows UI components to dismiss error messages after user acknowledgment.
     */
    fun clearError() {
        // Remove error message from UI state
        _uiState.update { it.copy(error = null) }
    }
}