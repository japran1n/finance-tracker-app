/**
 * Category Data Model and Default Categories
 * 
 * This file contains the Category data class and predefined default categories
 * for organizing financial transactions. Categories help users classify their
 * spending and income patterns for better financial tracking and reporting.
 * 
 * Features:
 * - User-specific categories with customizable properties
 * - Visual representation through colors and emoji icons
 * - Predefined categories for immediate app usability
 * - Extensible design for user-created custom categories
 */
package com.example.pageapp.data

/**
 * Category data class - represents a transaction classification category
 * 
 * Categories are used to organize and classify financial transactions,
 * making it easier for users to understand their spending patterns.
 * Each category has visual elements (color and icon) for better UI representation.
 * 
 * @property id Unique identifier for the category
 * @property name Display name of the category (e.g., "Food", "Transport")
 * @property color Hex color code for visual representation in charts and lists
 * @property icon Emoji or icon character for quick visual identification
 * @property userId Firebase user ID to associate category with specific user
 */
data class Category(
    // Primary key - unique identifier for each category
    // Default value 0 allows for auto-generation in database systems
    val id: Long = 0,
    
    // Human-readable name of the category
    // Examples: "Food", "Transport", "Salary", "Entertainment"
    val name: String,
    
    // Hex color code for visual representation
    // Used in UI elements like charts, category badges, and buttons
    // Format: "#RRGGBB" (e.g., "#FF6B6B" for red)
    val color: String,
    
    // Emoji or icon character for visual identification
    // Provides quick visual recognition in lists and selection screens
    // Examples: "🍽️" for food, "🚗" for transport
    val icon: String,
    
    // Firebase user ID to link category to specific user account
    // Empty string default for system-wide default categories
    val userId: String = ""
)

/**
 * Default Categories Object
 * 
 * Provides a predefined set of common financial categories that users
 * can immediately use without having to create their own. These cover
 * the most common expense and income types for personal finance tracking.
 * 
 * Categories are divided into:
 * - Expense categories (Food, Transport, Housing, etc.)
 * - Income categories (Salary, Freelance, Investment)
 * - General category (Other) for uncategorized transactions
 */
object DefaultCategories {
    // List of predefined categories with appropriate colors and icons
    // These categories are immediately available to all users
    val categories = listOf(
        // Expense Categories - common spending areas
        Category(name = "Food", color = "#FF6B6B", icon = "🍽️"), // Red - dining and groceries
        Category(name = "Transport", color = "#4ECDC4", icon = "🚗"), // Teal - vehicles and public transport
        Category(name = "Housing", color = "#45B7D1", icon = "🏠"), // Blue - rent, mortgage, utilities
        Category(name = "Entertainment", color = "#96CEB4", icon = "🎬"), // Green - movies, games, hobbies
        Category(name = "Healthcare", color = "#FFEAA7", icon = "🏥"), // Yellow - medical expenses
        Category(name = "Education", color = "#DDA0DD", icon = "📚"), // Purple - courses, books, training
        Category(name = "Shopping", color = "#FFB6C1", icon = "🛍️"), // Pink - clothing, electronics, misc items
        Category(name = "Utilities", color = "#F0E68C", icon = "⚡"), // Light yellow - electricity, water, internet
        
        // Income Categories - sources of money
        Category(name = "Salary", color = "#90EE90", icon = "💼"), // Light green - regular employment income
        Category(name = "Freelance", color = "#87CEEB", icon = "💻"), // Sky blue - freelance work income
        Category(name = "Investment", color = "#FFD700", icon = "📈"), // Gold - investment returns, dividends
        
        // General Category - catch-all for miscellaneous transactions
        Category(name = "Other", color = "#D3D3D3", icon = "📝") // Gray - uncategorized or unique transactions
    )
}