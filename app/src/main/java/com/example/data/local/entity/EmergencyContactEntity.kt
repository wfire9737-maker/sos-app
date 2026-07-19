package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey
    val contactId: String,
    val uid: String,
    val name: String,
    val phone: String,
    val relationship: String,
    val priority: Int
)
