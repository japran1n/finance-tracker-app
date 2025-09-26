/**
 * HomeScreen.kt - Main Dashboard UI Screen
 * 
 * This Compose UI screen serves as the primary dashboard for the finance app.
 * It displays financial summaries, transaction lists, and provides navigation
 * to other app features like adding transactions and accessing settings.
 * 
 * Key Features:
 * - Financial overview cards showing balance, income, and expenses
 * - Scrollable transaction list with edit/delete functionality
 * - Floating action button for quick transaction addition
 * - Top app bar with logout and settings navigation
 * - Responsive design with Material Design 3 components
 * - Long-press haptic feedback for transaction operations
 * 
 * UI Components:
 * - TopAppBar with navigation actions
 * - BalanceCard showing current financial status
 * - FinancialSummaryCards for income/expense overview
 * - LazyColumn for efficient transaction list rendering
 * - FloatingActionButton for quick access to add transactions
 */
package com.example.pageapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pageapp.data.Transaction
import com.example.pageapp.data.TransactionType
import com.example.pageapp.viewmodel.FinanceUiState
import java.text.NumberFormat
import java.util.*

/**
 * HomeScreen Composable - Main dashboard interface
 * 
 * This is the primary screen users see after logging in. It provides a comprehensive
 * overview of their financial status and quick access to transaction management.
 * 
 * @param uiState Current financial data and UI state from FinanceViewModel
 * @param onAddTransactionClick Callback triggered when user wants to add new transaction
 * @param onTransactionClick Callback triggered when user taps on a transaction (currently unused)
 * @param onDeleteTransaction Callback triggered when user deletes a transaction
 * @param onEditTransaction Callback triggered when user wants to edit a transaction
 * @param onLogoutClick Callback triggered when user wants to log out
 * @param onSettingsClick Callback triggered when user wants to access settings
 * @param currencySymbol Symbol to display before amounts (e.g., "$", "€", "R$")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    // Financial data and loading/error states from ViewModel
    uiState: FinanceUiState,
    // Navigation callback for adding new transactions
    onAddTransactionClick: () -> Unit,
    // Navigation callback for viewing transaction details (future feature)
    onTransactionClick: (Transaction) -> Unit,
    // Callback for deleting transactions with confirmation
    onDeleteTransaction: (Transaction) -> Unit,
    // Callback for editing existing transactions
    onEditTransaction: (Transaction) -> Unit,
    // Callback for user logout
    onLogoutClick: () -> Unit,
    // Navigation callback for accessing settings
    onSettingsClick: () -> Unit,
    // Currency symbol for formatting monetary amounts
    currencySymbol: String = "R$"
) {
    // Create currency formatter lambda using the provided currency symbol
    // This ensures consistent currency formatting throughout the screen
    val currencyFormatter = { amount: Double -> "$currencySymbol${"%.2f".format(amount)}" }

    // Main screen container using full available space
    Column(
        modifier = Modifier
            .fillMaxSize() // Use entire screen space
            .background(MaterialTheme.colorScheme.background) // Apply theme background color
    ) {
        // Top navigation bar with app title and action buttons
        CenterAlignedTopAppBar(
            title = {
                // App title displayed in the center of the top bar
                Text(
                    text = "Finance App",           // Application name
                    fontWeight = FontWeight.Bold,   // Bold text for emphasis
                    fontSize = 20.sp               // Large font size for visibility
                )
            },
            actions = {
                // Settings icon button - navigates to app settings
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Default.Settings,     // Material Design settings icon
                        contentDescription = "Settings", // Accessibility description
                        tint = MaterialTheme.colorScheme.onPrimary // Theme-aware icon color
                    )
                }
                // Logout icon button - signs out current user
                IconButton(onClick = onLogoutClick) {
                    Icon(
                        Icons.Default.ExitToApp,    // Material Design exit icon
                        contentDescription = "Logout", // Accessibility description
                        tint = MaterialTheme.colorScheme.onPrimary // Theme-aware icon color
                    )
                }
            },
            // Custom color scheme for the top app bar
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color(0xFF667eea),    // Purple-blue gradient background
                titleContentColor = Color.White,       // White title text
                actionIconContentColor = Color.White   // White action icons
            )
        )

        // Main content area using LazyColumn for efficient scrolling
        LazyColumn(
            modifier = Modifier.fillMaxSize(),              // Use remaining screen space
            contentPadding = PaddingValues(16.dp),          // Add padding around content
            verticalArrangement = Arrangement.spacedBy(16.dp) // Add consistent spacing between items
        ) {
            // Balance overview card - shows current financial status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),     // Use full available width
                    shape = RoundedCornerShape(16.dp),      // Rounded corners for modern look
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface // Theme-aware background
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Add shadow for depth
                ) {
                    // Card content container with padding and center alignment
                    Column(
                        modifier = Modifier.padding(20.dp),        // Internal padding for content
                        horizontalAlignment = Alignment.CenterHorizontally // Center all content horizontally
                    ) {
                        // Balance label text
                        Text(
                            text = "Current Balance",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // Subdued text color
                        )
                        
                        // Small spacing between label and amount
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Current balance amount with dynamic color based on positive/negative value
                        Text(
                            text = currencyFormatter(uiState.balance),  // Format balance with currency symbol
                            fontSize = 32.sp,                          // Large font for prominence
                            fontWeight = FontWeight.Bold,              // Bold for emphasis
                            color = if (uiState.balance >= 0) Color(0xFF4CAF50) else Color(0xFFE53E3E) // Green for positive, red for negative
                        )
                        
                        // Spacing before income/expense summary
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Row containing income and expense summary cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),              // Use full card width
                            horizontalArrangement = Arrangement.SpaceEvenly  // Distribute space evenly between items
                        ) {
                            // Income summary section
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally // Center align content
                            ) {
                                // Income label with trending up icon
                                Row(
                                    verticalAlignment = Alignment.CenterVertically // Align icon and text vertically
                                ) {
                                    Icon(
                                        Icons.Default.TrendingUp,        // Upward trending arrow icon
                                        contentDescription = null,       // Decorative icon, no description needed
                                        tint = Color(0xFF4CAF50),        // Green color for income
                                        modifier = Modifier.size(20.dp)  // Icon size
                                    )
                                    // Small space between icon and text
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Income",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // Subdued text
                                    )
                                }
                                // Total income amount
                                Text(
                                    text = currencyFormatter(uiState.totalIncome), // Format income with currency
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,     // Medium weight for readability
                                    color = Color(0xFF4CAF50)          // Green color matching icon
                                )
                            }
                            
                            // Expenses summary section
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally // Center align content
                            ) {
                                // Expense label with trending down icon
                                Row(
                                    verticalAlignment = Alignment.CenterVertically // Align icon and text vertically
                                ) {
                                    Icon(
                                        Icons.Default.TrendingDown,      // Downward trending arrow icon
                                        contentDescription = null,       // Decorative icon, no description needed
                                        tint = Color(0xFFE53E3E),        // Red color for expenses
                                        modifier = Modifier.size(20.dp)  // Icon size
                                    )
                                    // Small space between icon and text
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Expenses",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // Subdued text
                                    )
                                }
                                // Total expenses amount
                                Text(
                                    text = currencyFormatter(uiState.totalExpenses), // Format expenses with currency
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,     // Medium weight for readability
                                    color = Color(0xFFE53E3E)          // Red color matching icon
                                )
                            }
                        }
                    }
                }
            }

            // Add Transaction Button
            item {
                Button(
                    onClick = onAddTransactionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF667eea)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Transaction",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Recent Transactions
            item {
                Text(
                    text = "Recent Transactions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (uiState.transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📊",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No transactions yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Start by adding your first income or expense",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(uiState.transactions.take(10)) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) },
                        onEdit = { onEditTransaction(transaction) },
                        onDelete = { onDeleteTransaction(transaction) },
                        currencyFormatter = currencyFormatter
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    currencyFormatter: (Double) -> String
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMenu = true
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (transaction.type == TransactionType.INCOME) 
                                Color(0xFF4CAF50).copy(alpha = 0.1f) 
                            else 
                                Color(0xFFE53E3E).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (transaction.type == TransactionType.INCOME) 
                            Icons.Default.TrendingUp 
                        else 
                            Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (transaction.type == TransactionType.INCOME) 
                            Color(0xFF4CAF50) 
                        else 
                            Color(0xFFE53E3E),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Transaction Details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = transaction.description,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = transaction.category,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                // Amount
                Text(
                    text = currencyFormatter(transaction.amount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type == TransactionType.INCOME) 
                        Color(0xFF4CAF50) 
                    else 
                        Color(0xFFE53E3E)
                )
                
                // More options button
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}