package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.FallEvent

@Database(entities = [FallEvent::class], version = 1, exportSchema = false)
abstract class FallDatabase : RoomDatabase() {
    abstract fun fallEventDao(): FallEventDao

    companion object {
        @Volatile
        private var INSTANCE: FallDatabase? = null

        fun getDatabase(context: Context): FallDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FallDatabase::class.java,
                    "fall_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
