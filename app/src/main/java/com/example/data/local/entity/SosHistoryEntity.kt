package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sos_history")
data class SosHistoryEntity(
    @PrimaryKey
    val historyId: String,
    val uid: String,
    val latitude: Double,
    val longitude: Double,
    val googleMapsLink: String,
    val triggerSource: String,
    val date: Long,
    val status: String
)
