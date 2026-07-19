import os

os.makedirs("app/src/main/java/com/example/data/local/entity", exist_ok=True)
os.makedirs("app/src/main/java/com/example/data/local/dao", exist_ok=True)

with open("app/src/main/java/com/example/data/local/entity/UserEntity.kt", "w") as f:
    f.write("""package com.example.data.local.entity

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
""")

with open("app/src/main/java/com/example/data/local/entity/EmergencyContactEntity.kt", "w") as f:
    f.write("""package com.example.data.local.entity

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
""")

with open("app/src/main/java/com/example/data/local/entity/SosHistoryEntity.kt", "w") as f:
    f.write("""package com.example.data.local.entity

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
""")

with open("app/src/main/java/com/example/data/local/dao/UserDao.kt", "w") as f:
    f.write("""package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    fun getUserFlow(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUser(uid: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE uid = :uid")
    suspend fun deleteUser(uid: String)
    
    @Query("DELETE FROM users")
    suspend fun clearAll()
}
""")

with open("app/src/main/java/com/example/data/local/dao/EmergencyContactDao.kt", "w") as f:
    f.write("""package com.example.data.local.dao

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
""")

with open("app/src/main/java/com/example/data/local/dao/SosHistoryDao.kt", "w") as f:
    f.write("""package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SosHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SosHistoryDao {
    @Query("SELECT * FROM sos_history WHERE uid = :uid ORDER BY date DESC")
    fun getHistoryForUser(uid: String): Flow<List<SosHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: SosHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistories(histories: List<SosHistoryEntity>)

    @Query("DELETE FROM sos_history WHERE historyId = :historyId")
    suspend fun deleteHistory(historyId: String)
    
    @Query("DELETE FROM sos_history")
    suspend fun clearAll()
}
""")

with open("app/src/main/java/com/example/data/local/SmartSosDatabase.kt", "w") as f:
    f.write("""package com.example.data.local

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
""")

