/**
 * Repository Interface Definitions
 * 
 * This file contains the interface contracts for data repositories in the app.
 * These interfaces define the data operations without specifying implementation details,
 * allowing for different data sources (Firebase, Room, in-memory) while maintaining
 * consistent API contracts throughout the application.
 * 
 * Benefits:
 * - Abstraction layer between ViewModels and data sources
 * - Enables dependency injection and testing with mock implementations
 * - Supports multiple data source implementations
 * - Clean architecture separation of concerns
 */
package com.example.pageapp.data

import kotlinx.coroutines.flow.Flow

/**
 * Authentication Repository Interface
 * 
 * Defines the contract for user authentication operations including
 * sign in, sign up, sign out, and user state management.
 * Implementation classes handle the actual authentication logic with Firebase Auth.
 */
interface AuthRepository {
    /**
     * Observable flow of current authenticated user
     * Emits User object when logged in, null when logged out
     * ViewModels can observe this to react to authentication state changes
     */
    val currentUser: Flow<User?>
    
    /**
     * Sign in existing user with email and password
     * @param email User's email address
     * @param password User's password
     * @return Result containing User object on success, or error on failure
     */
    suspend fun signIn(email: String, password: String): Result<User>
    
    /**
     * Create new user account with email, password, and display name
     * @param email New user's email address
     * @param password New user's chosen password
     * @param displayName User's preferred display name
     * @return Result containing User object on success, or error on failure
     */
    suspend fun signUp(email: String, password: String, displayName: String): Result<User>
    
    /**
     * Sign out current user and clear authentication state
     * Updates currentUser flow to emit null
     */
    fun signOut()
    
    /**
     * Get current user's unique identifier
     * @return Firebase UID string if user is logged in, null otherwise
     */
    fun getCurrentUserId(): String?
    
    /**
     * Check if user is currently authenticated
     * @return true if user is logged in, false otherwise
     */
    fun isUserLoggedIn(): Boolean
}

/**
 * Transaction Repository Interface
 * 
 * Defines the contract for financial transaction data operations including
 * CRUD operations, querying, and financial calculations.
 * Implementation classes handle data persistence with Firebase Firestore or Room.
 */
interface TransactionRepository {
    /**
     * Get all transactions for a specific user as observable flow
     * @param userId Firebase user ID to filter transactions
     * @return Flow emitting list of transactions, updates automatically when data changes
     */
    fun getAllTransactions(userId: String): Flow<List<Transaction>>
    
    /**
     * Get transactions filtered by type (income or expense) for a specific user
     * @param userId Firebase user ID to filter transactions
     * @param type TransactionType enum (INCOME or EXPENSE)
     * @return Flow emitting filtered list of transactions
     */
    fun getTransactionsByType(userId: String, type: TransactionType): Flow<List<Transaction>>
    
    /**
     * Calculate total income amount for a specific user
     * @param userId Firebase user ID to calculate for
     * @return Sum of all income transactions as Double
     */
    suspend fun getTotalIncome(userId: String): Double
    
    /**
     * Calculate total expense amount for a specific user
     * @param userId Firebase user ID to calculate for
     * @return Sum of all expense transactions as Double
     */
    suspend fun getTotalExpenses(userId: String): Double
    
    /**
     * Calculate current balance (income minus expenses) for a specific user
     * @param userId Firebase user ID to calculate for
     * @return Current balance as Double (positive = surplus, negative = deficit)
     */
    suspend fun getBalance(userId: String): Double
    
    /**
     * Add new transaction to data store
     * @param transaction Transaction object to persist
     */
    suspend fun insertTransaction(transaction: Transaction)
    
    /**
     * Remove transaction from data store
     * @param transaction Transaction object to delete
     */
    suspend fun deleteTransaction(transaction: Transaction)
    
    /**
     * Update existing transaction in data store
     * @param transaction Transaction object with updated values
     */
    suspend fun updateTransaction(transaction: Transaction)
}