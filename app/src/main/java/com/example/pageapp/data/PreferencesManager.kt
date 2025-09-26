/**
 * PreferencesManager.kt - User Preferences and Settings Management
 * 
 * This utility class manages user preferences using Android SharedPreferences
 * with reactive StateFlow integration. It handles persistent storage of user
 * settings like theme preferences and currency selection.
 * 
 * Key Features:
 * - Reactive preference updates using StateFlow
 * - Persistent storage across app restarts
 * - Theme management (dark/light mode)
 * - Currency selection with symbol mapping
 * - Type-safe preference access
 * - Automatic state synchronization
 * 
 * Architecture:
 * - Uses SharedPreferences for data persistence
 * - Exposes StateFlow for reactive UI updates
 * - Provides convenient setter methods with automatic persistence
 * - Maps currency codes to display symbols
 */
package com.example.pageapp.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PreferencesManager - User settings and preferences controller
 * 
 * This class provides a centralized way to manage user preferences with
 * reactive updates and persistent storage. All preferences are automatically
 * saved to SharedPreferences and exposed as StateFlow for UI observation.
 * 
 * @param context Android context for accessing SharedPreferences
 */
class PreferencesManager(context: Context) {
    // SharedPreferences instance for persistent storage
    // Uses private mode to ensure data is only accessible by this app
    private val prefs: SharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    
    // Dark theme preference with reactive StateFlow
    // Loads saved value or defaults to false (light theme)
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", false))
    // Public read-only StateFlow for UI observation
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()
    
    // Currency preference with reactive StateFlow
    // Loads saved currency code or defaults to USD
    private val _currency = MutableStateFlow(prefs.getString("currency", "USD") ?: "USD")
    // Public read-only StateFlow for UI observation
    val currency: StateFlow<String> = _currency.asStateFlow()
    
    /**
     * Update dark theme preference
     * 
     * Saves the theme preference to SharedPreferences and updates the StateFlow
     * to notify all observers of the change.
     * 
     * @param isDark true for dark theme, false for light theme
     */
    fun setDarkTheme(isDark: Boolean) {
        // Persist the preference to SharedPreferences
        prefs.edit().putBoolean("dark_theme", isDark).apply()
        // Update StateFlow to notify UI components
        _isDarkTheme.value = isDark
    }
    
    /**
     * Update currency preference
     * 
     * Saves the currency selection to SharedPreferences and updates the StateFlow
     * to notify all observers of the change.
     * 
     * @param currency Currency code (e.g., "USD", "EUR", "GBP")
     */
    fun setCurrency(currency: String) {
        // Persist the preference to SharedPreferences
        prefs.edit().putString("currency", currency).apply()
        // Update StateFlow to notify UI components
        _currency.value = currency
    }
    
    /**
     * Get currency symbol for current currency setting
     * 
     * Maps the currently selected currency code to its corresponding symbol
     * for display in the UI. Supports major world currencies.
     * 
     * @return String symbol for the current currency (defaults to "$" for unknown currencies)
     */
    fun getCurrencySymbol(): String {
        return when (_currency.value) {
            "USD" -> "$"     // US Dollar
            "EUR" -> "€"     // Euro
            "BRL" -> "R$"    // Brazilian Real
            "GBP" -> "£"     // British Pound
            "JPY" -> "¥"     // Japanese Yen
            "CAD" -> "C$"    // Canadian Dollar
            "AUD" -> "A$"    // Australian Dollar
            "CHF" -> "CHF"   // Swiss Franc
            "CNY" -> "¥"     // Chinese Yuan
            "INR" -> "₹"     // Indian Rupee
            else -> "$"      // Default to USD symbol for unknown currencies
        }
    }
}