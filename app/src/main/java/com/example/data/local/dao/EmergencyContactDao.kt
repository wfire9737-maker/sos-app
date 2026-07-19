package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts WHERE uid = :uid ORDER BY priority ASC")
    fun getContactsForUser(uid: String): Flow<List<EmergencyContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<EmergencyContactEntity>)

    @Query("DELETE FROM emergency_contacts WHERE contactId = :contactId")
    suspend fun deleteContact(contactId: String)
    
    @Query("DELETE FROM emergency_contacts")
    suspend fun clearAll()
}
