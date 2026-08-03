package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.local.dao.TrustedPlaceDao
import com.example.data.local.entity.toDomainModel
import com.example.data.local.entity.toEntity
import com.example.model.TrustedPlace
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TrustedPlacesService(
    private val geofenceManager: GeofenceManager,
    private val context: Context,
    private val firestore: FirebaseFirestore?,
    private val trustedPlaceDao: TrustedPlaceDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _trustedPlaces = MutableStateFlow<List<TrustedPlace>>(emptyList())
    val trustedPlaces: StateFlow<List<TrustedPlace>> = _trustedPlaces.asStateFlow()
    
    private var currentUserId: String = ""

    fun initialize(userId: String) {
        currentUserId = userId
        if (userId.isNotBlank()) {
            scope.launch {
                loadFromLocal()
                syncFromCloud()
            }
        }
    }

    private suspend fun loadFromLocal() {
        try {
            trustedPlaceDao.getTrustedPlacesFlow(currentUserId).collect { entities ->
                _trustedPlaces.value = entities.map { it.toDomainModel() }
                geofenceManager.updateGeofences(_trustedPlaces.value)
            }
        } catch (e: Exception) {
            Log.e("TrustedPlacesService", "Failed to load local trusted places", e)
        }
    }

    private suspend fun syncFromCloud() {
        if (firestore == null || currentUserId.isBlank()) return
        try {
            val snapshot = firestore.collection("users").document(currentUserId)
                .collection("trusted_places").get().await()
                
            val places = snapshot.documents.mapNotNull { doc ->
                try {
                    TrustedPlace.fromMap(doc.data ?: emptyMap())
                } catch (e: Exception) {
                    null
                }
            }
            
            trustedPlaceDao.insertTrustedPlaces(places.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e("TrustedPlacesService", "Failed to sync trusted places from cloud", e)
        }
    }

    suspend fun addTrustedPlace(place: TrustedPlace) {
        val newPlace = place.copy(placeId = UUID.randomUUID().toString(), userId = currentUserId)
        try {
            trustedPlaceDao.insertTrustedPlace(newPlace.toEntity())
            firestore?.collection("users")?.document(currentUserId)
                ?.collection("trusted_places")?.document(newPlace.placeId)?.set(newPlace.toMap())
        } catch (e: Exception) {
            Log.e("TrustedPlacesService", "Failed to add trusted place", e)
        }
    }
    
    suspend fun updateTrustedPlace(place: TrustedPlace) {
        val updatedPlace = place.copy(lastUpdated = System.currentTimeMillis())
        try {
            trustedPlaceDao.insertTrustedPlace(updatedPlace.toEntity())
            firestore?.collection("users")?.document(currentUserId)
                ?.collection("trusted_places")?.document(updatedPlace.placeId)?.set(updatedPlace.toMap())
        } catch (e: Exception) {
            Log.e("TrustedPlacesService", "Failed to update trusted place", e)
        }
    }

    suspend fun deleteTrustedPlace(placeId: String) {
        try {
            trustedPlaceDao.deleteTrustedPlaceById(placeId)
            firestore?.collection("users")?.document(currentUserId)
                ?.collection("trusted_places")?.document(placeId)?.delete()
        } catch (e: Exception) {
            Log.e("TrustedPlacesService", "Failed to delete trusted place", e)
        }
    }
}
