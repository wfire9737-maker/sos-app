import os
import re

with open("app/src/main/java/com/example/di/AppModule.kt", "r") as f:
    content = f.read()

import_statement = """
import androidx.room.Room
import com.example.data.local.SmartSosDatabase
import com.example.data.local.dao.UserDao
import com.example.data.local.dao.EmergencyContactDao
import com.example.data.local.dao.SosHistoryDao
"""

provides_methods = """
    @Provides
    @Singleton
    fun provideSmartSosDatabase(@ApplicationContext context: Context): SmartSosDatabase {
        return Room.databaseBuilder(
            context,
            SmartSosDatabase::class.java,
            "smart_sos_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideUserDao(database: SmartSosDatabase): UserDao = database.userDao()

    @Provides
    fun provideEmergencyContactDao(database: SmartSosDatabase): EmergencyContactDao = database.emergencyContactDao()

    @Provides
    fun provideSosHistoryDao(database: SmartSosDatabase): SosHistoryDao = database.sosHistoryDao()
"""

# add imports
content = content.replace("import dagger.Module", import_statement + "\nimport dagger.Module")

# add provides
content = content.replace("}", provides_methods + "\n}")

with open("app/src/main/java/com/example/di/AppModule.kt", "w") as f:
    f.write(content)
