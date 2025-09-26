/**
 * MainActivity.kt - Main Entry Point of the Finance App
 * 
 * This is the single Activity that hosts all screens using Jetpack Compose.
 * It follows the single-activity architecture pattern recommended for modern Android apps.
 * 
 * Key Responsibilities:
 * - Initialize all repositories (Firebase or in-memory for testing)
 * - Set up navigation between screens
 * - Manage global app state (theme, preferences)
 * - Handle deep linking and intent processing
 * - Coordinate between ViewModels and UI
 */

package com.example.pageapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pageapp.data.*
import com.example.pageapp.ui.theme.PageAppTheme
import com.example.pageapp.ui.screens.*
import com.example.pageapp.viewmodel.*
import com.example.pageapp.data.PreferencesManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * MainActivity - Single Activity hosting all app screens
 * 
 * This activity uses Jetpack Compose for UI and Navigation Component for navigation.
 * It implements the Repository pattern for data access and MVVM for UI state management.
 */
class MainActivity : ComponentActivity() {
    
    /**
     * onCreate - Activity initialization
     * 
     * This method:
     * 1. Sets up the Compose UI
     * 2. Initializes repositories and ViewModels
     * 3. Configures navigation
     * 4. Applies theme based on user preferences
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            // Initialize preferences manager for app settings
            val preferencesManager = remember { PreferencesManager(this@MainActivity) }
            
            // Collect dark theme preference reactively
            val darkTheme by preferencesManager.isDarkTheme.collectAsState()
            
            // Repository Selection: Production vs Testing
            // In production, useFirebase should always be true
            // For local testing or offline development, can be set to false
            val useFirebase = true // Using production Firebase database
            
            // Initialize Authentication Repository
            // FirebaseAuthRepository: Uses Firebase Authentication service
            // InMemoryAuthRepository: For testing without Firebase connection
            val authRepository = if (useFirebase) {
                FirebaseAuthRepository()
            } else {
                InMemoryAuthRepository()
            }
            
            // Initialize Transaction Repository  
            // FirebaseTransactionRepository: Uses Firestore for real-time data sync
            // InMemoryTransactionRepository: For testing with local memory storage
            val transactionRepository = if (useFirebase) {
                FirebaseTransactionRepository()
            } else {
                InMemoryTransactionRepository()
            }
            
            // Apply app theme based on user preferences
            // PageAppTheme: Custom Material Design 3 theme with dark/light mode support
            PageAppTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Launch the main app composition
                    FinanceApp(
                        authRepository = authRepository,
                        transactionRepository = transactionRepository,
                        preferencesManager = preferencesManager
                    )
                }
            }
        }
    }
}

/**
 * FinanceApp - Main app composition function
 * 
 * This composable:
 * 1. Sets up navigation controller
 * 2. Initializes ViewModels with dependency injection
 * 3. Manages app-wide state (authentication, splash screen)
 * 4. Defines navigation graph between screens
 * 
 * @param authRepository Repository for authentication operations
 * @param transactionRepository Repository for transaction data operations  
 * @param preferencesManager Manager for app preferences and settings
 */
@Composable
fun FinanceApp(
    authRepository: AuthRepository,
    transactionRepository: TransactionRepository,
    preferencesManager: PreferencesManager
) {
    // Navigation controller for managing screen transitions
    val navController = rememberNavController()
    
    // Initialize ViewModels with manual dependency injection
    // AuthViewModel: Manages authentication state and operations
    val authViewModel: AuthViewModel = viewModel { AuthViewModel(authRepository) }
    
    // FinanceViewModel: Manages financial data and transaction operations
    val financeViewModel: FinanceViewModel = viewModel { 
        FinanceViewModel(transactionRepository, authRepository) 
    }
    
    // Reactive authentication state - triggers UI updates when user signs in/out
    val currentUser by authRepository.currentUser.collectAsState(initial = null)
    
    // First-time user flag - could be expanded for onboarding flow
    // First-time user flag - could be expanded for onboarding flow
    // This state determines if we should show onboarding screens to new users
    val isFirstTime = remember { mutableStateOf(false) }
    
    // Splash screen state management
    // Controls whether the splash screen is currently visible
    var showSplash by remember { mutableStateOf(true) }
    
    // Splash screen timer - shows for 2 seconds on app launch
    // LaunchedEffect runs when this composable is first composed
    // Unit key means it runs only once during the composable lifecycle
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000) // 2 second splash duration - gives time for app initialization
        showSplash = false // Hide splash screen after delay
    }
    
    // Conditional UI rendering based on splash screen state
    if (showSplash) {
        // Show splash screen during app initialization
        SplashScreen(
            onSplashFinished = { showSplash = false } // Callback to manually finish splash
        )
    } else {
        // Determine which screen to show first based on user state
        // Navigation logic: onboarding -> login -> home
        val startDestination = when {
            isFirstTime.value -> "onboarding"  // New user sees onboarding
            currentUser == null -> "login"     // Not logged in user sees login
            else -> "home"                     // Logged in user goes to home
        }
        
        // Navigation Host - manages all screen navigation in the app
        // This is the main navigation container for Jetpack Compose Navigation
        NavHost(
            navController = navController,     // Controller for navigation actions
            startDestination = startDestination // Initial screen to display
        ) {
            // Onboarding Screen Route Definition
            // This route handles first-time user introduction
            composable("onboarding") {
                OnboardingScreen(
                    // Callback when user completes onboarding
                    onComplete = {
                        // Navigate to login and remove onboarding from back stack
                        navController.navigate("login") {
                            popUpTo("onboarding") { inclusive = true } // Clear onboarding from stack
                        }
                    }
                )
            }
            
            // Login/Registration Screen Route Definition
            // Handles user authentication (sign in and sign up)
            composable("login") {
                LoginScreen(
                    // Sign in callback - triggered when user attempts to log in
                    onSignIn = { email, password ->
                        authViewModel.signIn(email, password) // Delegate to ViewModel
                    },
                    // Sign up callback - triggered when user creates new account
                    onSignUp = { email, password, displayName ->
                        authViewModel.signUp(email, password, displayName) // Delegate to ViewModel
                    },
                    // Loading state - shows spinner during authentication
                    isLoading = authViewModel.uiState.collectAsState().value.isLoading,
                    // Error state - shows error messages if authentication fails
                    error = authViewModel.uiState.collectAsState().value.error
                )
                
                // Listen for authentication state changes
                // When user successfully logs in, navigate to home screen
                LaunchedEffect(currentUser) {
                    if (currentUser != null) { // User is now authenticated
                        // Navigate to home and clear login from back stack
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true } // Prevent back to login
                        }
                    }
                }
            }
            
            // Home Screen Route Definition - Main dashboard of the app
            // Shows financial overview, recent transactions, and navigation options
            composable("home") {
                // Collect financial data state from ViewModel
                val uiState by financeViewModel.uiState.collectAsState()
                
                HomeScreen(
                    uiState = uiState, // Current financial data and loading state
                    // Navigation callback to add new transaction
                    onAddTransactionClick = {
                        navController.navigate("add_transaction") // Go to add transaction screen
                    },
                    // Callback when user taps on a transaction (currently unused)
                    onTransactionClick = { transaction ->
                        // Navigate to transaction details if needed (placeholder for future feature)
                    },
                    // Delete transaction callback - removes transaction from database
                    onDeleteTransaction = { transaction ->
                        financeViewModel.deleteTransaction(transaction) // Delegate to ViewModel
                    },
                    // Edit transaction callback - opens edit form with existing data
                    onEditTransaction = { transaction ->
                        // Set the transaction to edit in the ViewModel state
                        financeViewModel.setTransactionToEdit(transaction)
                        // Navigate to the same form used for adding (but pre-filled for editing)
                        navController.navigate("add_transaction")
                    },
                    // Logout callback - signs out user and returns to login
                    onLogoutClick = {
                        authViewModel.signOut() // Clear authentication state
                        // Navigate to login and clear home from back stack
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true } // Prevent back to home when logged out
                        }
                    },
                    // Settings navigation callback
                    onSettingsClick = {
                        navController.navigate("settings") // Go to settings screen
                    },
                    // Currency symbol for displaying amounts - reactively updates when currency changes
                    currencySymbol = getCurrencySymbol(preferencesManager.currency.collectAsState().value)
                )
            }
            
            // Add/Edit Transaction Screen Route Definition
            // Dual-purpose screen: adding new transactions or editing existing ones
            composable("add_transaction") {
                // Collect current currency preference for display
                val currentCurrency by preferencesManager.currency.collectAsState()
                // Collect finance UI state to check if we're editing
                val uiState by financeViewModel.uiState.collectAsState()
                // Extract transaction being edited (null if adding new)
                val transactionToEdit = uiState.transactionToEdit
                
                AddTransactionScreen(
                    // Save callback - handles both add and edit operations
                    onSaveTransaction = { amount, description, category, type ->
                        if (transactionToEdit != null) {
                            // Edit mode: update existing transaction with new data
                            financeViewModel.editTransaction(transactionToEdit, amount, description, category, type)
                        } else {
                            // Add mode: create new transaction with provided data
                            financeViewModel.addTransaction(amount, description, category, type)
                        }
                        // Clear edit state - reset transaction being edited to null
                        financeViewModel.setTransactionToEdit(null) 
                        // Return to previous screen (home)
                        navController.popBackStack()
                    },
                    // Back button callback - cancels operation and returns to home
                    onBackClick = {
                        // Clear edit state if user cancels
                        financeViewModel.setTransactionToEdit(null) 
                        // Return to previous screen without saving
                        navController.popBackStack()
                    },
                    // Current currency symbol for amount display
                    currencySymbol = getCurrencySymbol(currentCurrency),
                    // Pass existing transaction data for editing (null for new transaction)
                    transactionToEdit = transactionToEdit
                )
            }
            
            // Settings Screen Route Definition
            // Allows users to configure app preferences and account settings
            composable("settings") {
                // Get context for potential system operations (like file export)
                val context = LocalContext.current
                // Collect dark theme preference from data store
                val darkTheme by preferencesManager.isDarkTheme.collectAsState()
                // Collect current currency preference from data store
                val currentCurrency by preferencesManager.currency.collectAsState()
                
                SettingsScreen(
                    // Current dark theme setting for toggle display
                    isDarkTheme = darkTheme,
                    // Back navigation callback - returns to home screen
                    onBackClick = {
                        navController.popBackStack() // Return to previous screen
                    },
                    // Theme toggle callback - updates dark/light mode preference
                    onThemeChange = { isEnabled ->
                        preferencesManager.setDarkTheme(isEnabled) // Update theme preference
                    },
                    // Current currency for displaying in settings
                    currentCurrency = currentCurrency,
                    // Currency change callback - updates user preference
                    onCurrencyChange = { currency ->
                        preferencesManager.setCurrency(currency) // Update currency preference
                    },
                    // PDF export callback - placeholder for future feature
                    onExportPDF = {
                        // TODO: Implement PDF export functionality for financial reports
                        // Show toast notification that feature is not yet implemented
                        Toast.makeText(context, "PDF export not implemented yet", Toast.LENGTH_SHORT).show()
                    },
                    // CSV export callback - exports transaction data to CSV file
                    onExportCSV = {
                        // Call CSV export function with current ViewModel and context
                        exportTransactionsToCSV(financeViewModel, context)
                    }
                )
            }
        }
    }
}

// Utility function to get currency symbol from currency code
// Maps currency codes to their respective symbols for display purposes
private fun getCurrencySymbol(currency: String): String {
    return when (currency) {
        "USD" -> "$"       // US Dollar
        "EUR" -> "€"       // Euro
        "GBP" -> "£"       // British Pound
        "JPY" -> "¥"       // Japanese Yen
        "CAD" -> "C$"      // Canadian Dollar
        "AUD" -> "A$"      // Australian Dollar
        "CHF" -> "CHF"     // Swiss Franc
        "CNY" -> "¥"       // Chinese Yuan
        "INR" -> "₹"       // Indian Rupee
        "KRW" -> "₩"       // South Korean Won
        else -> "$"        // Default to US Dollar symbol
    }
}

// Function to export transactions to CSV format
// Takes ViewModel and context to handle file operations and user feedback
fun exportTransactionsToCSV(financeViewModel: FinanceViewModel, context: Context) {
    try {
        // Generate CSV content from ViewModel
        val csvContent = financeViewModel.exportTransactionsToCSV()
        // Check if there are transactions to export
        if (csvContent == "No transactions to export") {
            // Show user feedback if no data available
            Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
            return // Exit function early if no data
        }
        
        // Create filename with current date and time for uniqueness
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val fileName = "transactions_${dateFormatter.format(Date())}.csv"
        
        // Save to Downloads directory (publicly accessible location)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        
        // Write CSV content to file using FileWriter
        FileWriter(file).use { writer ->
            writer.write(csvContent) // Write the generated CSV string to file
        }
        
        // Create file URI for sharing using FileProvider (required for Android 7.0+)
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider", // Authority defined in manifest
            file
        )
        
        // Create share intent to allow user to share the CSV file
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv" // MIME type for CSV files
            putExtra(Intent.EXTRA_STREAM, fileUri) // Attach the file
            putExtra(Intent.EXTRA_SUBJECT, "Finance App - Transaction Export") // Email subject
            putExtra(Intent.EXTRA_TEXT, "Your transaction data exported from Finance App") // Email body
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant read permission to receiving app
        }
        
        // Show chooser dialog for user to select sharing method (email, drive, etc.)
        context.startActivity(Intent.createChooser(shareIntent, "Share CSV file"))
        // Show success message to user
        Toast.makeText(context, "CSV exported to Downloads and ready to share", Toast.LENGTH_LONG).show()
        
    } catch (e: Exception) {
        // Handle any errors during file operations
        Toast.makeText(context, "Error exporting CSV: ${e.message}", Toast.LENGTH_LONG).show()
    }
}