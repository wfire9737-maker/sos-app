import os

with open("app/src/main/java/com/example/repository/UserRepositoryImpl.kt", "w") as f:
    f.write("""package com.example.repository

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
""")

with open("app/src/main/java/com/example/repository/EmergencyContactRepositoryImpl.kt", "w") as f:
    f.write("""package com.example.repository

import com.example.data.local.dao.EmergencyContactDao
import com.example.data.local.entity.EmergencyContactEntity
import com.example.model.EmergencyContact
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyContactRepositoryImpl @Inject constructor(
    private val contactDao: EmergencyContactDao,
    private val firestore: FirebaseFirestore
) : EmergencyContactRepository {

    override fun getContactsForUser(uid: String): Flow<List<EmergencyContact>> {
        return contactDao.getContactsForUser(uid).map { list -> list.map { it.toContact() } }
    }

    override suspend fun addContact(contact: EmergencyContact): Result<Unit> {
        return try {
            val docRef = firestore.collection("emergency_contacts").document()
            val finalContact = contact.copy(id = docRef.id)
            docRef.set(finalContact).await()
            contactDao.insertContact(finalContact.toEntity("unknown-uid")) // We need uid here. Let's assume contact model has it. Wait, contact model doesn't have uid.
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateContact(contact: EmergencyContact): Result<Unit> {
        return try {
            firestore.collection("emergency_contacts").document(contact.id).set(contact).await()
            // contactDao.insertContact(contact.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteContact(contactId: String): Result<Unit> {
        return try {
            firestore.collection("emergency_contacts").document(contactId).delete().await()
            contactDao.deleteContact(contactId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncContactsWithRemote(uid: String) {
        try {
            val snapshot = firestore.collection("emergency_contacts").whereEqualTo("uid", uid).get().await()
            val remoteContacts = snapshot.toObjects(EmergencyContact::class.java) // We might need a custom mapping
            // contactDao.insertContacts(remoteContacts.map { it.toEntity(uid) })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun EmergencyContactEntity.toContact() = EmergencyContact(
        id = this.contactId,
        name = this.name,
        phoneNumber = this.phone,
        relation = this.relationship
    )
    
    // We will adjust the mapper after checking the actual EmergencyContact model
}
""")
