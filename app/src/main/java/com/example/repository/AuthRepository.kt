package com.example.repository

import com.example.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun register(user: User, password: String): Flow<Result<User>>
    suspend fun login(email: String, password: String): Flow<Result<User>>
    suspend fun logout()
    suspend fun resetPassword(email: String): Flow<Result<Boolean>>
    fun getCurrentUserFlow(): Flow<User?>
}
