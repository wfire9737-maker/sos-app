package com.example.repository

import android.util.Log
import com.example.data.local.dao.EmergencyContactDao
import com.example.data.local.entity.EmergencyContactEntity
import com.example.model.EmergencyContact
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

    private fun getAuthenticatedUid(): String? {
        return try {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid.isNullOrBlank() || uid.startsWith("demo-", ignoreCase = true) || uid == "user-101" || uid == "anonymous") {
                null
            } else {
                uid
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun getContactsForUser(uid: String): Flow<List<EmergencyContact>> {
        return contactDao.getContactsForUser(uid).map { list -> list.map { it.toContact() } }
    }

    override suspend fun addContact(contact: EmergencyContact): Result<Unit> {
        val authUid = getAuthenticatedUid() ?: contact.userId.takeIf { it.isNotBlank() && !it.startsWith("demo-") }
        val finalContact = if (contact.id.isBlank()) {
            contact.copy(
                id = "contact-" + java.util.UUID.randomUUID().toString().take(8),
                userId = authUid ?: contact.userId
            )
        } else {
            contact.copy(userId = authUid ?: contact.userId)
        }

        return try {
            val entity = EmergencyContactEntity(
                contactId = finalContact.id,
                uid = finalContact.userId,
                name = finalContact.name,
                phone = finalContact.phone,
                relationship = finalContact.relationship,
                priority = finalContact.priority,
                customSmsTemplate = finalContact.customSmsTemplate
            )
            contactDao.insertContact(entity)

            if (authUid != null) {
                firestore.collection("users").document(authUid)
                    .collection("contacts").document(finalContact.id)
                    .set(finalContact.toMap(), SetOptions.merge())
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EmergencyContactRepo", "Failed to add contact: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updateContact(contact: EmergencyContact): Result<Unit> {
        val authUid = getAuthenticatedUid() ?: contact.userId.takeIf { it.isNotBlank() && !it.startsWith("demo-") }
        val finalContact = contact.copy(userId = authUid ?: contact.userId)
        return try {
            val entity = EmergencyContactEntity(
                contactId = finalContact.id,
                uid = finalContact.userId,
                name = finalContact.name,
                phone = finalContact.phone,
                relationship = finalContact.relationship,
                priority = finalContact.priority,
                customSmsTemplate = finalContact.customSmsTemplate
            )
            contactDao.insertContact(entity)

            if (authUid != null) {
                firestore.collection("users").document(authUid)
                    .collection("contacts").document(finalContact.id)
                    .set(finalContact.toMap(), SetOptions.merge())
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EmergencyContactRepo", "Failed to update contact: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun deleteContact(contactId: String): Result<Unit> {
        val authUid = getAuthenticatedUid()
        return try {
            contactDao.deleteContact(contactId)

            if (authUid != null) {
                firestore.collection("users").document(authUid)
                    .collection("contacts").document(contactId)
                    .delete()
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EmergencyContactRepo", "Failed to delete contact: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun syncContactsWithRemote(uid: String) {
        val authUid = getAuthenticatedUid() ?: uid.takeIf { it.isNotBlank() && !it.startsWith("demo-") && it != "user-101" } ?: return
        try {
            val snapshot = firestore.collection("users").document(authUid)
                .collection("contacts").get().await()
            val remoteContacts = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val id = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: doc.id
                    val raw = EmergencyContact.fromMap(data)
                    raw.copy(id = id, userId = authUid)
                } catch (e: Exception) {
                    null
                }
            }
            for (contact in remoteContacts) {
                if (contact.id.isNotBlank()) {
                    val entity = EmergencyContactEntity(
                        contactId = contact.id,
                        uid = authUid,
                        name = contact.name,
                        phone = contact.phone,
                        relationship = contact.relationship,
                        priority = contact.priority,
                        customSmsTemplate = contact.customSmsTemplate
                    )
                    contactDao.insertContact(entity)
                }
            }
        } catch (e: Exception) {
            Log.e("EmergencyContactRepo", "Failed to sync contacts with remote: ${e.message}")
        }
    }

    private fun EmergencyContactEntity.toContact() = EmergencyContact(
        id = this.contactId,
        userId = this.uid,
        name = this.name,
        phone = this.phone,
        relationship = this.relationship,
        priority = this.priority,
        customSmsTemplate = this.customSmsTemplate
    )
}
