/**
 * Transaction Data Model
 * 
 * This data class represents a financial transaction in the app.
 * It serves as the core data structure for all financial operations including
 * income and expense tracking, database storage, and UI display.
 * 
 * Features:
 * - Immutable data structure using Kotlin data class
 * - Compatible with both Room database and Firebase Firestore
 * - Supports user-specific transactions through userId field
 * - String-based date storage for cross-platform compatibility
 */
package com.example.pageapp.data

/**
 * Transaction data class - represents a single financial transaction
 * 
 * This is the primary data model for financial transactions in the app.
 * Each transaction contains all necessary information for tracking income and expenses.
 * 
 * @property id Unique identifier for the transaction (auto-generated)
 * @property amount Monetary value of the transaction (always positive, type determines income/expense)
 * @property description User-provided description explaining the transaction purpose
 * @property category Classification category (e.g., "Food", "Transport", "Salary")
 * @property type Enum indicating whether this is income or expense
 * @property dateTime Transaction timestamp stored as ISO string for database compatibility
 * @property userId Firebase user ID to associate transaction with specific user account
 */
data class Transaction(
    // Primary key - unique identifier for each transaction
    // Default value 0 allows for auto-generation in database systems
    val id: Long = 0,
    
    // Transaction amount in user's selected currency
    // Always stored as positive value - type field determines income vs expense
    val amount: Double,
    
    // User-provided description of the transaction
    // Examples: "Grocery shopping", "Salary payment", "Gas bill"
    val description: String,
    
    // Category classification for organizing transactions
    // Helps users track spending patterns and generate reports
    val category: String,
    
    // Type enumeration - determines if this adds or subtracts from balance
    val type: TransactionType,
    
    // Date and time when transaction occurred
    // Stored as string in ISO format for Room database compatibility
    // Example format: "2024-01-15T10:30:00Z"
    val dateTime: String, 
    
    // Firebase user ID to link transaction to specific user account
    // Empty string default for backwards compatibility
    val userId: String = ""
)

/**
 * Transaction Type Enumeration
 * 
 * Defines the two possible types of financial transactions in the app.
 * This enumeration is used to distinguish between money coming in (income)
 * and money going out (expense) for accurate balance calculations.
 */
enum class TransactionType {
    // Income - money received (salary, gifts, refunds, etc.)
    // Adds to the user's total balance
    INCOME,
    
    // Expense - money spent (purchases, bills, fees, etc.)  
    // Subtracts from the user's total balance
    EXPENSE
}