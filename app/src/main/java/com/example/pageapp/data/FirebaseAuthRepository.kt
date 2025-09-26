/**
 * FirebaseAuthRepository.kt - Firebase Authentication Implementation
 * 
 * This class provides Firebase Authentication integration for the finance app.
 * It implements the AuthRepository interface to handle user authentication,
 * registration, and session management using Google Firebase Auth services.
 * 
 * Key Features:
 * - Real-time authentication state monitoring
 * - Secure email/password authentication
 * - User profile management with display names
 * - Automatic session persistence across app restarts
 * - Error handling and logging for debugging
 * 
 * Security:
 * - Passwords are handled securely by Firebase Auth
 * - User sessions are managed server-side
 * - Automatic token refresh for long-lived sessions
 */
package com.example.pageapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await

/**
 * FirebaseAuthRepository - Production authentication implementation
 * 
 * This repository integrates with Firebase Authentication to provide
 * secure user management for the finance application.
 */
class FirebaseAuthRepository : AuthRepository {
    // Firebase Authentication instance - handles all auth operations
    private val auth = FirebaseAuth.getInstance()
    
    // Mutable state flow for internal user state management
    // Allows the repository to update user state when authentication changes
    private val _currentUser = MutableStateFlow<User?>(null)
    
    // Public observable flow of current user state
    // ViewModels can observe this to react to authentication changes
    override val currentUser: Flow<User?> = _currentUser
    
    /**
     * Repository initialization
     * Sets up Firebase Auth state listener for automatic user state updates
     */
    init {
        println("FirebaseAuthRepository: Initializing Firebase Auth")
        // Observe auth state changes - triggered on login, logout, and app restart
        auth.addAuthStateListener { firebaseAuth ->
            // Get current Firebase user (null if not authenticated)
            val firebaseUser = firebaseAuth.currentUser
            println("FirebaseAuthRepository: Auth state changed - User: ${firebaseUser?.uid}")
            
            // Update internal state flow with current user
            _currentUser.value = firebaseUser?.let { 
                // Convert Firebase user to app User object
                User(
                    id = it.uid,                    // Firebase unique user ID
                    email = it.email ?: "",        // User's email address
                    displayName = it.displayName ?: "" // User's display name (may be empty)
                )
            } // null if no user is authenticated
        }
    }
    
    /**
     * Sign in existing user with email and password
     * 
     * @param email User's registered email address
     * @param password User's password
     * @return Result<User> containing User object on success, exception on failure
     */
    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            println("FirebaseAuthRepository: Attempting sign in for email: $email")
            // Perform Firebase authentication (suspends until complete)
            val result = auth.signInWithEmailAndPassword(email, password).await()
            // Extract Firebase user from authentication result
            val firebaseUser = result.user
            
            if (firebaseUser != null) {
                println("FirebaseAuthRepository: Sign in successful for user: ${firebaseUser.uid}")
                // Create app User object from Firebase user data
                val user = User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: ""
                )
                // Return successful result with user data
                Result.success(user)
            } else {
                println("FirebaseAuthRepository: Sign in failed - no user returned")
                // Authentication failed - no user object returned
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            println("FirebaseAuthRepository: Sign in error - ${e.message}")
            // Return failure result with exception details
            Result.failure(e)
        }
    }
    
    /**
     * Sign up new user with email, password, and display name
     * 
     * Creates a new Firebase Auth account and sets the user's display name.
     * This is a two-step process: account creation followed by profile update.
     * 
     * @param email New user's email address (must be valid and unique)
     * @param password New user's chosen password (must meet Firebase requirements)
     * @param displayName User's preferred display name for the app
     * @return Result<User> containing User object on success, exception on failure
     */
    override suspend fun signUp(email: String, password: String, displayName: String): Result<User> {
        return try {
            println("FirebaseAuthRepository: Attempting sign up for email: $email")
            // Create new Firebase Auth account with email and password
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            // Extract Firebase user from creation result
            val firebaseUser = result.user
            
            if (firebaseUser != null) {
                println("FirebaseAuthRepository: Account created, updating profile with display name: $displayName")
                // Create profile update request with display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName) // Set user's chosen display name
                    .build()
                // Apply profile updates to Firebase user (suspends until complete)
                firebaseUser.updateProfile(profileUpdates).await()
                
                println("FirebaseAuthRepository: Sign up successful for user: ${firebaseUser.uid}")
                // Create app User object with all user data
                val user = User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = displayName // Use the display name we just set
                )
                // Return successful result with user data
                Result.success(user)
            } else {
                println("FirebaseAuthRepository: Sign up failed - no user returned")
                // Account creation failed - no user object returned
                Result.failure(Exception("Registration failed"))
            }
        } catch (e: Exception) {
            println("FirebaseAuthRepository: Sign up error - ${e.message}")
            // Return failure result with exception details
            Result.failure(e)
        }
    }
    
    /**
     * Sign out current user
     * 
     * Clears the Firebase authentication session and triggers
     * the auth state listener to update the currentUser flow to null.
     */
    override fun signOut() {
        println("FirebaseAuthRepository: Signing out current user")
        // Firebase handles session cleanup and state updates
        auth.signOut()
    }
    
    /**
     * Get current authenticated user's unique identifier
     * 
     * @return String user ID if authenticated, null if not logged in
     */
    override fun getCurrentUserId(): String? {
        // Get Firebase user ID from current authentication state
        val uid = auth.currentUser?.uid
        println("FirebaseAuthRepository: getCurrentUserId() returning: $uid")
        return uid
    }
    
    /**
     * Check if user is currently authenticated
     * 
     * @return true if user is logged in, false otherwise
     */
    override fun isUserLoggedIn(): Boolean {
        // Simple check for current user existence
        val isLoggedIn = auth.currentUser != null
        println("FirebaseAuthRepository: isUserLoggedIn() returning: $isLoggedIn")
        return isLoggedIn
    }
}