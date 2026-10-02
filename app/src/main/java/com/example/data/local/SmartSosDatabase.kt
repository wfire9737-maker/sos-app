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
    version = 7,
    exportSchema = false
)
abstract class SmartSosDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun locationDao(): LocationDao
    abstract fun sosHistoryDao(): SosHistoryDao
    abstract fun trustedPlaceDao(): TrustedPlaceDao

    companion object {
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE emergency_contacts ADD COLUMN customSmsTemplate TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trusted_places ADD COLUMN skipAutomaticSms INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trusted_places ADD COLUMN isEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sos_history ADD COLUMN durationSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sos_history ADD COLUMN address TEXT NOT NULL DEFAULT 'GPS Coordinate Plot'")
                db.execSQL("ALTER TABLE sos_history ADD COLUMN severity TEXT NOT NULL DEFAULT 'HIGH'")
                db.execSQL("ALTER TABLE sos_history ADD COLUMN contactsNotified TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sos_history ADD COLUMN deviceUsed TEXT NOT NULL DEFAULT 'MOBILE-APP-SOS'")
                db.execSQL("ALTER TABLE sos_history ADD COLUMN resolutionNotes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sos_history ADD COLUMN resolvedBy TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sos_history ADD COLUMN aiConfidence INTEGER NOT NULL DEFAULT 90")
            }
        }
    }
}
