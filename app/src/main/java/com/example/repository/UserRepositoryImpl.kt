package com.example.repository

import com.example.data.local.dao.UserDao
import com.example.data.local.entity.UserEntity
import com.example.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore
) : UserRepository {

    override fun getUserFlow(uid: String): Flow<User?> {
        return userDao.getUserFlow(uid).map { it?.toUser() }
    }

    override suspend fun saveUserLocally(user: User) {
        userDao.insertUser(user.toEntity())
    }

    override suspend fun syncUserWithRemote(uid: String) {
        try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            val user = snapshot.toObject(User::class.java)
            if (user != null) {
                userDao.insertUser(user.toEntity())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        return try {
            firestore.collection("users").document(user.uid).set(user).await()
            userDao.insertUser(user.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Mapping extensions
    private fun User.toEntity() = UserEntity(
        uid = this.uid,
        fullName = this.name,
        phone = this.phone,
        email = this.email,
        bloodGroup = this.bloodType,
        medicalCondition = this.medicalInfo,
        address = "", // Add mapping if address exists in User
        profilePhotoUrl = this.photoUri ?: "",
        createdAt = this.createdAt
    )

    private fun UserEntity.toUser() = User(
        uid = this.uid,
        name = this.fullName,
        email = this.email,
        phone = this.phone,
        medicalInfo = this.medicalCondition,
        bloodType = this.bloodGroup,
        photoUri = this.profilePhotoUrl,
        createdAt = this.createdAt
    )
}
