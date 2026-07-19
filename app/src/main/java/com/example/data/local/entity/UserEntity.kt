package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val fullName: String,
    val phone: String,
    val email: String,
    val bloodGroup: String,
    val medicalCondition: String,
    val address: String,
    val profilePhotoUrl: String,
    val createdAt: Long
)
