package com.example.repository

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
            val entity = com.example.data.local.entity.EmergencyContactEntity(
                contactId = finalContact.id,
                uid = finalContact.userId,
                name = finalContact.name,
                phone = finalContact.phone,
                relationship = finalContact.relationship,
                priority = finalContact.priority
            )
            contactDao.insertContact(entity)
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
        userId = this.uid,
        name = this.name,
        phone = this.phone,
        relationship = this.relationship,
        priority = this.priority
    )
    
    // We will adjust the mapper after checking the actual EmergencyContact model
}
