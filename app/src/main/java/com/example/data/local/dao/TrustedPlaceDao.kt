package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.TrustedPlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedPlaceDao {
    @Query("SELECT * FROM trusted_places WHERE userId = :userId")
    fun getTrustedPlacesFlow(userId: String): Flow<List<TrustedPlaceEntity>>

    @Query("SELECT * FROM trusted_places WHERE userId = :userId")
    suspend fun getTrustedPlaces(userId: String): List<TrustedPlaceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrustedPlace(place: TrustedPlaceEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrustedPlaces(places: List<TrustedPlaceEntity>)

    @Delete
    suspend fun deleteTrustedPlace(place: TrustedPlaceEntity)
    
    @Query("DELETE FROM trusted_places WHERE placeId = :placeId")
    suspend fun deleteTrustedPlaceById(placeId: String)
}
