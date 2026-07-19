package com.example.repository

import com.example.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserFlow(uid: String): Flow<User?>
    suspend fun saveUserLocally(user: User)
    suspend fun syncUserWithRemote(uid: String)
    suspend fun updateUser(user: User): Result<Unit>
}
