package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_history")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val accuracy: Float,
    val timestamp: Long,
    val address: String
)
