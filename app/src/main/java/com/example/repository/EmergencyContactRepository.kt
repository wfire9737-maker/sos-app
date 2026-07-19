package com.example.repository

import com.example.model.EmergencyContact
import kotlinx.coroutines.flow.Flow

interface EmergencyContactRepository {
    fun getContactsForUser(uid: String): Flow<List<EmergencyContact>>
    suspend fun addContact(contact: EmergencyContact): Result<Unit>
    suspend fun updateContact(contact: EmergencyContact): Result<Unit>
    suspend fun deleteContact(contactId: String): Result<Unit>
    suspend fun syncContactsWithRemote(uid: String)
}
