/**
 * User Data Model
 * 
 * This data class represents a user account in the finance app.
 * It stores user authentication and profile information received from Firebase Auth.
 * This model is used throughout the app for user identification and personalization.
 * 
 * Features:
 * - Compatible with Firebase Authentication user properties
 * - Immutable data structure for thread safety
 * - Default empty values for easy initialization
 * - Profile picture support for future UI enhancements
 */
package com.example.pageapp.data

/**
 * User data class - represents an authenticated user account
 * 
 * This is the core user model containing essential account information.
 * All properties have default empty string values to facilitate object creation
 * and prevent null pointer exceptions throughout the application.
 * 
 * @property id Unique user identifier from Firebase Authentication
 * @property email User's email address used for login and identification
 * @property displayName User's chosen display name for personalization
 * @property profilePictureUrl URL to user's profile image (future feature)
 */
data class User(
    // Firebase Authentication UID - unique identifier for each user
    // Used to associate transactions and preferences with specific accounts
    val id: String = "",
    
    // User's email address from Firebase Auth
    // Primary identifier for login and account recovery
    val email: String = "",
    
    // User's display name for personalized UI elements
    // Can be different from email, allows for friendly greetings
    val displayName: String = "",
    
    // URL to user's profile picture (placeholder for future enhancement)
    // Could integrate with Firebase Storage or external image services
    val profilePictureUrl: String = ""
)