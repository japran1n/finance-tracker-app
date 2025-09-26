/**
 * FinanceViewModel.kt - Finance Business Logic and State Management
 * 
 * This ViewModel manages all financial data and business logic for the app.
 * It serves as the intermediary between UI screens and data repositories,
 * handling transaction operations, financial calculations, and state management.
 * 
 * Key Responsibilities:
 * - Transaction CRUD operations (Create, Read, Update, Delete)
 * - Financial calculations (balance, totals, summaries)
 * - UI state management with loading and error states
 * - Authentication-aware data loading
 * - CSV export functionality
 * - Edit transaction state management
 * 
 * Architecture:
 * - Follows MVVM pattern with reactive state flows
 * - Integrates with Repository pattern for data access
 * - Provides reactive UI state updates
 * - Handles coroutine lifecycle management
 */
package com.example.pageapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pageapp.data.Transaction
import com.example.pageapp.data.TransactionType
import com.example.pageapp.data.AuthRepository
import com.example.pageapp.data.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.text.SimpleDateFormat
import java.util.*

/**
 * FinanceUiState - Represents the complete UI state for finance screens
 * 
 * This data class encapsulates all the state information needed by UI components
 * to render financial data, loading states, and handle user interactions.
 * 
 * @property transactions List of all user transactions for display
 * @property balance Current account balance (income minus expenses)
 * @property totalIncome Sum of all income transactions
 * @property totalExpenses Sum of all expense transactions
 * @property isLoading Boolean indicating if data operations are in progress
 * @property error Error message string for displaying user-friendly error information
 * @property transactionToEdit Transaction object currently being edited (null if adding new)
 */
data class FinanceUiState(
    // List of transactions to display in UI (sorted by most recent first)
    val transactions: List<Transaction> = emptyList(),
    // Current account balance - positive means surplus, negative means deficit
    val balance: Double = 0.0,
    // Total income amount from all income transactions
    val totalIncome: Double = 0.0,
    // Total expense amount from all expense transactions
    val totalExpenses: Double = 0.0,
    // Loading state for showing progress indicators during data operations
    val isLoading: Boolean = false,
    // Error message for displaying when operations fail
    val error: String? = null,
    // Transaction being edited (null when adding new transaction)
    val transactionToEdit: Transaction? = null
)

/**
 * FinanceViewModel - Core business logic controller for financial operations
 * 
 * This ViewModel manages the financial state of the application and provides
 * methods for transaction management, financial calculations, and data export.
 * It automatically responds to authentication state changes and manages data lifecycle.
 * 
 * @param transactionRepository Repository for transaction data operations
 * @param authRepository Repository for authentication state monitoring
 */
class FinanceViewModel(
    // Repository for accessing and modifying transaction data
    private val transactionRepository: TransactionRepository,
    // Repository for monitoring user authentication state
    private val authRepository: AuthRepository
) : ViewModel() {

    // Private mutable state flow for internal state updates
    private val _uiState = MutableStateFlow(FinanceUiState())
    // Public read-only state flow for UI observation
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()
    
    // Job reference for data collection operations (allows cancellation)
    private var dataCollectionJob: Job? = null

    /**
     * ViewModel initialization
     * Sets up authentication state monitoring and automatic data loading
     */
    init {
        println("FinanceViewModel: Initializing...")
        // Launch coroutine to observe authentication state changes
        viewModelScope.launch {
            // Collect authentication state changes (login/logout events)
            authRepository.currentUser.collect { user ->
                println("FinanceViewModel: Auth state changed - User: ${user?.id}")
                
                // Cancel any ongoing data collection to prevent memory leaks
                dataCollectionJob?.cancel()
                
                if (user != null) {
                    // User is authenticated - load their financial data
                    loadData()
                } else {
                    // User is not authenticated - clear all financial data
                    println("FinanceViewModel: No user logged in, clearing data")
                    _uiState.value = FinanceUiState() // Reset to empty state
                }
            }
        }
    }

    /**
     * Load financial data for the current authenticated user
     * 
     * This private method handles the loading of transaction data and financial calculations.
     * It sets up reactive data collection from the repository and updates UI state accordingly.
     */
    private fun loadData() {
        // Get current user ID from authentication repository
        val userId = authRepository.getCurrentUserId()
        println("FinanceViewModel: Loading data for user: $userId")
        
        if (userId == null) {
            // No authenticated user - cannot load data
            println("FinanceViewModel: No user logged in, skipping data load")
            _uiState.update { it.copy(isLoading = false, error = "No user logged in") }
            return
        }
        
        // Cancel any existing data collection job to prevent multiple active collections
        dataCollectionJob?.cancel()
        
        // Start new data collection job for the authenticated user
        dataCollectionJob = viewModelScope.launch {
            // Set loading state to show progress indicators in UI
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                println("FinanceViewModel: Starting to collect transactions for user: $userId")
                // Set up reactive data collection from transaction repository
                // This flow will emit new data whenever transactions change in the database
                transactionRepository.getAllTransactions(userId)
                    .collect { transactions ->
                        println("FinanceViewModel: Received ${transactions.size} transactions from repository")
                        
                        // Calculate total income from all income transactions
                        // Filter for income type transactions and sum their amounts
                        val totalIncome = transactions
                            .filter { it.type == TransactionType.INCOME }
                            .sumOf { it.amount }
                        
                        // Calculate total expenses from all expense transactions  
                        // Filter for expense type transactions and sum their amounts
                        val totalExpenses = transactions
                            .filter { it.type == TransactionType.EXPENSE }
                            .sumOf { it.amount }
                        
                        // Calculate current balance (income minus expenses)
                        // Positive balance = surplus, negative balance = deficit
                        val balance = totalIncome - totalExpenses
                        
                        println("FinanceViewModel: Calculated - Income: $totalIncome, Expenses: $totalExpenses, Balance: $balance")
                        
                        // Update UI state with calculated financial data
                        _uiState.value = FinanceUiState(
                            transactions = transactions,    // All user transactions
                            balance = balance,              // Current account balance
                            totalIncome = totalIncome,      // Sum of all income
                            totalExpenses = totalExpenses,  // Sum of all expenses
                            isLoading = false,              // Clear loading state
                            error = null                    // Clear any previous errors
                        )
                    }
            } catch (e: Exception) {
                // Handle any errors during data loading
                println("FinanceViewModel: Error loading data - ${e.message}")
                e.printStackTrace()
                // Update UI state with error information
                _uiState.update { 
                    it.copy(
                        isLoading = false,    // Stop loading indicator
                        error = e.message     // Show error message to user
                    )
                }
            }
        }
    }
    
    /**
     * Manually refresh financial data
     * 
     * This method allows UI components to trigger a data refresh,
     * useful for pull-to-refresh functionality or retry operations.
     */
    fun refreshData() {
        println("FinanceViewModel: Manual refresh requested")
        // Delegate to loadData method which handles the actual data loading
        loadData()
    }

    /**
     * Add new transaction to the database
     * 
     * Creates a new transaction with the provided details and saves it to the database.
     * Automatically generates transaction ID and timestamp, associates with current user.
     * 
     * @param amount Transaction amount (always positive, type determines income/expense)
     * @param description User-provided description of the transaction
     * @param category Selected category for organizing the transaction
     * @param type TransactionType enum (INCOME or EXPENSE)
     */
    fun addTransaction(
        amount: Double,
        description: String,
        category: String,
        type: TransactionType
    ) {
        // Get current authenticated user ID
        val userId = authRepository.getCurrentUserId()
        println("FinanceViewModel: STARTING ADD TRANSACTION for user: $userId")
        println("FinanceViewModel: Transaction details - Amount: $amount, Description: $description, Type: $type")
        
        if (userId == null) {
            // Cannot add transaction without authenticated user
            println("FinanceViewModel: ERROR - No user logged in, cannot add transaction")
            _uiState.update { it.copy(error = "Please log in to add transactions") }
            return
        }
        
        // Launch coroutine for database operation
        viewModelScope.launch {
            try {
                // Create date formatter for consistent timestamp format
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                // Generate unique transaction ID using current timestamp
                val transactionId = System.currentTimeMillis()
                
                // Create transaction object with all required data
                val transaction = Transaction(
                    id = transactionId,                    // Unique identifier
                    amount = amount,                       // Transaction amount
                    description = description,             // User description
                    category = category,                   // Selected category
                    type = type,                          // Income or expense
                    dateTime = sdf.format(Date()),        // Current timestamp as string
                    userId = userId                       // Associate with current user
                )
                println("FinanceViewModel: Created transaction object with ID: $transactionId")
                println("FinanceViewModel: About to call insertTransaction...")
                
                // Save transaction to database through repository
                transactionRepository.insertTransaction(transaction)
                println("FinanceViewModel: insertTransaction completed successfully!")
                
                // Clear any previous error messages after successful operation
                _uiState.update { it.copy(error = null) }
                
                // Force refresh data to ensure UI updates immediately
                // Note: Repository flow should auto-update, but explicit refresh ensures consistency
                println("FinanceViewModel: Forcing data refresh after transaction insert")
                loadData()
                
            } catch (e: Exception) {
                // Handle any errors during transaction creation
                println("FinanceViewModel: ERROR adding transaction - ${e.message}")
                println("FinanceViewModel: Exception type: ${e.javaClass.simpleName}")
                e.printStackTrace()
                // Update UI state with user-friendly error message
                _uiState.update { it.copy(error = "Failed to add transaction: ${e.message}") }
            }
        }
    }

    /**
     * Delete existing transaction from the database
     * 
     * Removes the specified transaction from the database and refreshes the UI data.
     * Includes comprehensive error handling and logging for debugging.
     * 
     * @param transaction Transaction object to be deleted
     */
    fun deleteTransaction(transaction: Transaction) {
        println("FinanceViewModel: DELETE CALLED - ${transaction.description} with ID: ${transaction.id}")
        
        // Launch coroutine for database operation
        viewModelScope.launch {
            try {
                println("FinanceViewModel: About to call repository.deleteTransaction")
                // Delete transaction from database through repository
                transactionRepository.deleteTransaction(transaction)
                println("FinanceViewModel: Repository deleteTransaction call completed")
                
                // Force a manual refresh to ensure UI updates immediately
                // Repository flow should auto-update, but explicit refresh ensures data consistency
                println("FinanceViewModel: Forcing manual data refresh after delete")
                loadData()
                
            } catch (e: Exception) {
                // Handle any errors during transaction deletion
                println("FinanceViewModel: Error deleting transaction - ${e.message}")
                e.printStackTrace()
                // Update UI state with user-friendly error message
                _uiState.update { it.copy(error = "Failed to delete transaction: ${e.message}") }
            }
        }
    }
    
    /**
     * Edit existing transaction with new data
     * 
     * Updates an existing transaction with new values while preserving
     * the original ID, timestamp, and user association.
     * 
     * @param originalTransaction The transaction to be updated
     * @param amount New transaction amount
     * @param description New transaction description
     * @param category New transaction category
     * @param type New transaction type (income/expense)
     */
    fun editTransaction(
        originalTransaction: Transaction,
        amount: Double,
        description: String,
        category: String,
        type: TransactionType
    ) {
        println("FinanceViewModel: EDIT CALLED - ${originalTransaction.description} with ID: ${originalTransaction.id}")
        
        // Launch coroutine for database operation
        viewModelScope.launch {
            try {
                // Create updated transaction preserving original metadata
                val updatedTransaction = originalTransaction.copy(
                    amount = amount,           // Update with new amount
                    description = description, // Update with new description
                    category = category,       // Update with new category
                    type = type               // Update with new type
                    // Keep the same ID, dateTime, and userId from original transaction
                )
                
                println("FinanceViewModel: Created updated transaction - ID: ${updatedTransaction.id}, Description: ${updatedTransaction.description}")
                println("FinanceViewModel: About to call repository.updateTransaction")
                
                // Use updateTransaction to modify existing record (preserves ID and timestamp)
                transactionRepository.updateTransaction(updatedTransaction)
                
                println("FinanceViewModel: Repository updateTransaction call completed")
                
                // Force a manual refresh to ensure UI updates immediately
                // Repository flow should auto-update, but explicit refresh ensures data consistency
                println("FinanceViewModel: Forcing manual data refresh after edit")
                loadData()
                
                // Note: Data will automatically refresh due to Flow observation from repository
            } catch (e: Exception) {
                // Handle any errors during transaction editing
                println("FinanceViewModel: Error editing transaction - ${e.message}")
                e.printStackTrace()
                // Update UI state with user-friendly error message
                _uiState.update { it.copy(error = "Failed to edit transaction: ${e.message}") }
            }
        }
    }

    /**
     * Clear any error messages from UI state
     * 
     * Allows UI components to dismiss error messages after user acknowledgment.
     */
    fun clearError() {
        // Remove error message from UI state
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Set transaction for editing mode
     * 
     * Stores a transaction in the UI state for editing. Used by the AddTransactionScreen
     * to pre-populate form fields when editing an existing transaction.
     * 
     * @param transaction Transaction to edit (null to clear edit mode)
     */
    fun setTransactionToEdit(transaction: Transaction?) {
        // Update UI state with transaction to edit (null clears edit mode)
        _uiState.update { it.copy(transactionToEdit = transaction) }
    }
    
    /**
     * Export transactions to CSV format
     * 
     * Generates a CSV string containing all user transactions with proper formatting.
     * Includes headers and handles special characters in descriptions and categories.
     * 
     * @return String containing CSV data or error message if no transactions exist
     */
    fun exportTransactionsToCSV(): String {
        // Get current transactions from UI state
        val transactions = _uiState.value.transactions
        if (transactions.isEmpty()) {
            // Return message if no transactions to export
            return "No transactions to export"
        }
        
        // Create CSV header row with column names
        val csvHeader = "Date,Description,Category,Type,Amount\n"
        
        // Convert each transaction to CSV row format
        val csvContent = transactions.joinToString("\n") { transaction ->
            // Format: Date,"Description","Category",Type,Amount
            // Descriptions and categories are quoted to handle commas and special characters
            "${transaction.dateTime},\"${transaction.description}\",\"${transaction.category}\",${transaction.type},${transaction.amount}"
        }
        
        // Combine header and content into complete CSV string
        return csvHeader + csvContent
    }
    
    fun getCurrentTransactions(): List<Transaction> {
        return _uiState.value.transactions
    }
    
    override fun onCleared() {
        super.onCleared()
        dataCollectionJob?.cancel()
        println("FinanceViewModel: ViewModel cleared, data collection job cancelled")
    }
}