import os

os.makedirs("app/src/main/java/com/example/repository", exist_ok=True)

with open("app/src/main/java/com/example/repository/AuthRepository.kt", "w") as f:
    f.write("""package com.example.repository

import com.example.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun register(user: User, password: String): Flow<Result<User>>
    suspend fun login(email: String, password: String): Flow<Result<User>>
    suspend fun logout()
    suspend fun resetPassword(email: String): Flow<Result<Boolean>>
    fun getCurrentUserFlow(): Flow<User?>
}
""")

with open("app/src/main/java/com/example/repository/UserRepository.kt", "w") as f:
    f.write("""package com.example.repository

import com.example.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserFlow(uid: String): Flow<User?>
    suspend fun saveUserLocally(user: User)
    suspend fun syncUserWithRemote(uid: String)
    suspend fun updateUser(user: User): Result<Unit>
}
""")

with open("app/src/main/java/com/example/repository/EmergencyContactRepository.kt", "w") as f:
    f.write("""package com.example.repository

import com.example.model.EmergencyContact
import kotlinx.coroutines.flow.Flow

interface EmergencyContactRepository {
    fun getContactsForUser(uid: String): Flow<List<EmergencyContact>>
    suspend fun addContact(contact: EmergencyContact): Result<Unit>
    suspend fun updateContact(contact: EmergencyContact): Result<Unit>
    suspend fun deleteContact(contactId: String): Result<Unit>
    suspend fun syncContactsWithRemote(uid: String)
}
""")

with open("app/src/main/java/com/example/repository/SosHistoryRepository.kt", "w") as f:
    f.write("""package com.example.repository

import com.example.model.EmergencyHistoryItem
import kotlinx.coroutines.flow.Flow

interface SosHistoryRepository {
    fun getHistoryForUser(uid: String): Flow<List<EmergencyHistoryItem>>
    suspend fun addHistoryItem(item: EmergencyHistoryItem): Result<Unit>
    suspend fun syncHistoryWithRemote(uid: String)
}
""")

