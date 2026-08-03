package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.dao.EmergencyContactDao
import com.example.data.local.dao.SosHistoryDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.EmergencyContactEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.local.dao.LocationDao
import com.example.data.local.entity.SosHistoryEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.TrustedPlaceEntity
import com.example.data.local.dao.TrustedPlaceDao

@Database(
    entities = [
        UserEntity::class,
        EmergencyContactEntity::class,
        SosHistoryEntity::class,
        LocationEntity::class,
        TrustedPlaceEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SmartSosDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun locationDao(): LocationDao
    abstract fun sosHistoryDao(): SosHistoryDao
    abstract fun trustedPlaceDao(): TrustedPlaceDao
}
