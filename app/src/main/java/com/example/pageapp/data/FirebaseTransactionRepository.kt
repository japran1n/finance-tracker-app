/**
 * FirebaseTransactionRepository.kt - Firebase Firestore Implementation
 * 
 * This class provides the Firebase implementation of the TransactionRepository interface.
 * It handles all CRUD operations for transactions using Google Cloud Firestore.
 * 
 * Key Features:
 * - Real-time data synchronization using Firestore listeners
 * - User-specific data isolation through userId filtering
 * - Automatic conflict resolution and offline support
 * - Optimized queries for performance
 * 
 * Security:
 * - All operations require authenticated users
 * - Server-side security rules enforce data access controls
 * - User data is completely isolated from other users
 */

package com.example.pageapp.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * FirebaseTransactionRepository - Production database implementation
 * 
 * This repository connects to Google Cloud Firestore for production data storage.
 * It implements the Repository pattern to abstract Firebase operations from the UI layer.
 */
class FirebaseTransactionRepository : TransactionRepository {
    
    // Firebase Firestore instance - connects to cloud database
    private val db = FirebaseFirestore.getInstance()
    
    // Reference to the "transactions" collection in Firestore
    private val transactionsCollection = db.collection("transactions")
    
    /**
     * Repository initialization
     * Sets up connection to Firestore and logs initialization for debugging
     */
    init {
        println("FirebaseTransactionRepository: Initialized with Firestore instance")
    }
    
    /**
     * getAllTransactions - Real-time transaction stream for a specific user
     * 
     * This method creates a Flow that emits transaction lists whenever data changes
     * in Firestore. It uses Firestore's snapshot listeners for real-time updates.
     * 
     * @param userId The ID of the user whose transactions to retrieve
     * @return Flow<List<Transaction>> A reactive stream of transaction lists
     * 
     * How it works:
     * 1. Creates a Firestore query filtered by userId
     * 2. Attaches a snapshot listener for real-time updates
     * 3. Converts Firestore documents to Transaction objects
     * 4. Emits the transaction list through the Flow
     * 5. Automatically cleans up listener when Flow is closed
     */
    override fun getAllTransactions(userId: String): Flow<List<Transaction>> = callbackFlow {
        println("FirebaseTransactionRepository: STARTING getAllTransactions for user: $userId")
        
        // Create Firestore listener for real-time updates
        // This query filters transactions by userId to ensure data privacy
        val listener = transactionsCollection
            .whereEqualTo("userId", userId)  // Only get this user's transactions
            .addSnapshotListener { snapshot, exception ->
                // Handle any errors in the Firestore operation
                if (exception != null) {
                    println("FirebaseTransactionRepository: ERROR getting transactions - ${exception.message}")
                    exception.printStackTrace()
                    close(exception)  // Close the Flow with error
                    return@addSnapshotListener
                }
                
                println("FirebaseTransactionRepository: Snapshot received, documents count: ${snapshot?.documents?.size ?: 0}")
                
                val transactions = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        println("FirebaseTransactionRepository: Processing document ID: ${doc.id}")
                        println("FirebaseTransactionRepository: Document data: ${doc.data}")
                        val docIdString = doc.getString("id")
                        println("FirebaseTransactionRepository: Transaction ID from document: $docIdString")
                        
                        val transaction = Transaction(
                            id = doc.getString("id")?.toLongOrNull() ?: System.currentTimeMillis(),
                            amount = doc.getDouble("amount") ?: 0.0,
                            description = doc.getString("description") ?: "",
                            category = doc.getString("category") ?: "",
                            type = TransactionType.valueOf(doc.getString("type") ?: "EXPENSE"),
                            dateTime = doc.getString("dateTime") ?: "",
                            userId = doc.getString("userId") ?: ""
                        )
                        println("FirebaseTransactionRepository: Parsed transaction - ID: ${transaction.id}, Description: ${transaction.description}")
                        println("FirebaseTransactionRepository: Document ID vs Transaction ID - Doc: ${doc.id}, Trans: ${transaction.id}")
                        transaction
                    } catch (e: Exception) {
                        println("FirebaseTransactionRepository: Error parsing transaction from document ${doc.id} - ${e.message}")
                        null
                    }
                }?.sortedByDescending { it.id } ?: emptyList() // Sort by transaction ID instead of timestamp
                
                println("FirebaseTransactionRepository: Sending ${transactions.size} transactions to Flow")
                val success = trySend(transactions)
                println("FirebaseTransactionRepository: Flow send result: ${success.isSuccess}")
            }
        
        awaitClose { 
            println("FirebaseTransactionRepository: Closing getAllTransactions listener")
            listener.remove() 
        }
    }
    
    override fun getTransactionsByType(userId: String, type: TransactionType): Flow<List<Transaction>> = callbackFlow {
        val listener = transactionsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("type", type.name)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    close(exception)
                    return@addSnapshotListener
                }
                
                val transactions = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Transaction(
                            id = doc.getString("id")?.toLongOrNull() ?: System.currentTimeMillis(),
                            amount = doc.getDouble("amount") ?: 0.0,
                            description = doc.getString("description") ?: "",
                            category = doc.getString("category") ?: "",
                            type = TransactionType.valueOf(doc.getString("type") ?: "EXPENSE"),
                            dateTime = doc.getString("dateTime") ?: "",
                            userId = doc.getString("userId") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(transactions)
            }
        
        awaitClose { listener.remove() }
    }
    
    override suspend fun getTotalIncome(userId: String): Double {
        return try {
            val snapshot = transactionsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", TransactionType.INCOME.name)
                .get()
                .await()
            
            snapshot.documents.sumOf { doc -> 
                doc.getDouble("amount") ?: 0.0 
            }
        } catch (e: Exception) {
            0.0
        }
    }
    
    override suspend fun getTotalExpenses(userId: String): Double {
        return try {
            val snapshot = transactionsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", TransactionType.EXPENSE.name)
                .get()
                .await()
            
            snapshot.documents.sumOf { doc -> 
                doc.getDouble("amount") ?: 0.0 
            }
        } catch (e: Exception) {
            0.0
        }
    }
    
    override suspend fun getBalance(userId: String): Double {
        val income = getTotalIncome(userId)
        val expenses = getTotalExpenses(userId)
        return income - expenses
    }
    
    override suspend fun insertTransaction(transaction: Transaction) {
        try {
            println("FirebaseTransactionRepository: ===== INSERT TRANSACTION START =====")
            println("FirebaseTransactionRepository: Transaction ID: ${transaction.id}")
            println("FirebaseTransactionRepository: Description: ${transaction.description}")
            println("FirebaseTransactionRepository: Amount: ${transaction.amount}")
            println("FirebaseTransactionRepository: Type: ${transaction.type}")
            println("FirebaseTransactionRepository: User ID: ${transaction.userId}")
            
            val transactionData = hashMapOf(
                "id" to transaction.id.toString(),
                "amount" to transaction.amount,
                "description" to transaction.description,
                "category" to transaction.category,
                "type" to transaction.type.name,
                "dateTime" to transaction.dateTime,
                "userId" to transaction.userId,
                "timestamp" to System.currentTimeMillis()
            )
            
            println("FirebaseTransactionRepository: Prepared data: $transactionData")
            
            // Add the document to Firestore (let Firebase generate the document ID)
            val documentRef = transactionsCollection.add(transactionData).await()
            println("FirebaseTransactionRepository: Document added with Firebase ID: ${documentRef.id}")
            println("FirebaseTransactionRepository: ===== INSERT TRANSACTION END =====")
            
        } catch (e: Exception) {
            println("FirebaseTransactionRepository: ERROR inserting transaction - ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    override suspend fun deleteTransaction(transaction: Transaction) {
        try {
            println("FirebaseTransactionRepository: ===== DELETE TRANSACTION START =====")
            println("FirebaseTransactionRepository: Transaction ID: ${transaction.id}")
            println("FirebaseTransactionRepository: Transaction Description: ${transaction.description}")
            println("FirebaseTransactionRepository: User ID: ${transaction.userId}")
            
            // Get all documents for this user and find the one that matches our transaction
            val querySnapshot = transactionsCollection
                .whereEqualTo("userId", transaction.userId)
                .get()
                .await()
            
            println("FirebaseTransactionRepository: Found ${querySnapshot.documents.size} total documents for user")
            
            var deletedCount = 0
            for (document in querySnapshot.documents) {
                val docTransactionId = document.getString("id")
                val docDescription = document.getString("description")
                println("FirebaseTransactionRepository: Checking document ${document.id} - TransID: $docTransactionId, Desc: $docDescription")
                
                if (docTransactionId == transaction.id.toString()) {
                    println("FirebaseTransactionRepository: MATCH FOUND! Deleting document ${document.id}")
                    document.reference.delete().await()
                    deletedCount++
                    println("FirebaseTransactionRepository: Document deleted successfully")
                }
            }
            
            println("FirebaseTransactionRepository: Deleted $deletedCount documents")
            if (deletedCount == 0) {
                println("FirebaseTransactionRepository: WARNING - No matching documents found to delete")
            }
            println("FirebaseTransactionRepository: ===== DELETE TRANSACTION END =====")
            
        } catch (e: Exception) {
            println("FirebaseTransactionRepository: ERROR in deleteTransaction - ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    override suspend fun updateTransaction(transaction: Transaction) {
        try {
            println("FirebaseTransactionRepository: ===== UPDATE TRANSACTION START =====")
            println("FirebaseTransactionRepository: Transaction ID: ${transaction.id}")
            println("FirebaseTransactionRepository: Transaction Description: ${transaction.description}")
            println("FirebaseTransactionRepository: User ID: ${transaction.userId}")
            
            // Get all documents for this user and find the one that matches our transaction
            val querySnapshot = transactionsCollection
                .whereEqualTo("userId", transaction.userId)
                .get()
                .await()
            
            println("FirebaseTransactionRepository: Found ${querySnapshot.documents.size} total documents for user")
            
            var updatedCount = 0
            for (document in querySnapshot.documents) {
                val docTransactionId = document.getString("id")
                val docDescription = document.getString("description")
                println("FirebaseTransactionRepository: Checking document ${document.id} - TransID: $docTransactionId")
                
                if (docTransactionId == transaction.id.toString()) {
                    println("FirebaseTransactionRepository: MATCH FOUND! Updating document ${document.id}")
                    
                    val transactionData = hashMapOf(
                        "id" to transaction.id.toString(),
                        "amount" to transaction.amount,
                        "description" to transaction.description,
                        "category" to transaction.category,
                        "type" to transaction.type.name,
                        "dateTime" to transaction.dateTime,
                        "userId" to transaction.userId,
                        "timestamp" to System.currentTimeMillis()
                    )
                    
                    document.reference.set(transactionData).await()
                    updatedCount++
                    println("FirebaseTransactionRepository: Document updated successfully")
                    break // Only update the first match
                }
            }
            
            println("FirebaseTransactionRepository: Updated $updatedCount documents")
            if (updatedCount == 0) {
                println("FirebaseTransactionRepository: WARNING - No matching documents found to update")
                throw Exception("Transaction not found for update")
            }
            println("FirebaseTransactionRepository: ===== UPDATE TRANSACTION END =====")
            
        } catch (e: Exception) {
            println("FirebaseTransactionRepository: ERROR in updateTransaction - ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}