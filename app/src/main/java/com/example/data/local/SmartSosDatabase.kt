package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.dao.EmergencyContactDao
import com.example.data.local.dao.SosHistoryDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.EmergencyContactEntity
import com.example.data.local.entity.SosHistoryEntity
import com.example.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        EmergencyContactEntity::class,
        SosHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SmartSosDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun sosHistoryDao(): SosHistoryDao
}
