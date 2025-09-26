package com.example.pageapp.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class InMemoryTransactionRepository : TransactionRepository {
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    
    override fun getAllTransactions(userId: String): Flow<List<Transaction>> =
        _transactions.map { list -> list.filter { it.userId == userId }.sortedByDescending { it.dateTime } }
    
    override fun getTransactionsByType(userId: String, type: TransactionType): Flow<List<Transaction>> =
        _transactions.map { list -> 
            list.filter { it.userId == userId && it.type == type }.sortedByDescending { it.dateTime }
        }
    
    override suspend fun getTotalIncome(userId: String): Double =
        _transactions.value.filter { it.userId == userId && it.type == TransactionType.INCOME }
            .sumOf { it.amount }
    
    override suspend fun getTotalExpenses(userId: String): Double =
        _transactions.value.filter { it.userId == userId && it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
    
    override suspend fun getBalance(userId: String): Double {
        val income = getTotalIncome(userId)
        val expenses = getTotalExpenses(userId)
        return income - expenses
    }
    
    override suspend fun insertTransaction(transaction: Transaction) {
        val currentList = _transactions.value.toMutableList()
        val newTransaction = transaction.copy(id = System.currentTimeMillis())
        currentList.add(newTransaction)
        _transactions.value = currentList
    }
    
    override suspend fun deleteTransaction(transaction: Transaction) {
        val currentList = _transactions.value.toMutableList()
        currentList.removeAll { it.id == transaction.id }
        _transactions.value = currentList
    }
    
    override suspend fun updateTransaction(transaction: Transaction) {
        val currentList = _transactions.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            currentList[index] = transaction
            _transactions.value = currentList
        }
    }
}

class InMemoryAuthRepository : AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUser
    
    // For demo purposes, we'll simulate authentication
    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val user = User(
                id = "demo_user_${email.hashCode()}",
                email = email,
                displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
            )
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun signUp(email: String, password: String, displayName: String): Result<User> {
        return try {
            val user = User(
                id = "demo_user_${email.hashCode()}",
                email = email,
                displayName = displayName
            )
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun signOut() {
        _currentUser.value = null
    }
    
    override fun getCurrentUserId(): String? = _currentUser.value?.id
    
    override fun isUserLoggedIn(): Boolean = _currentUser.value != null
}