package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.local.dao.TrustedPlaceDao
import com.example.data.local.entity.toDomainModel
import com.example.data.local.entity.toEntity
import com.example.model.TrustedPlace
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _trustedPlaces = MutableStateFlow<List<TrustedPlace>>(emptyList())
    val trustedPlaces: StateFlow<List<TrustedPlace>> = _trustedPlaces.asStateFlow()
    
    private var currentUserId: String = ""
    private var sessionJob: Job? = null

    fun initialize(userId: String) {
        sessionJob?.cancel()
        sessionJob = null

        currentUserId = userId

        if (userId.isBlank()) {
            _trustedPlaces.value = emptyList()
            try {
                geofenceManager.updateGeofences(emptyList())
            } catch (e: Exception) {
                // Ignore geofence reset exceptions
            }
            return
        }

        val sessionUserId = userId
        sessionJob = scope.launch {
            launch {
                syncFromCloud(sessionUserId)
            }
            loadFromLocal(sessionUserId)
        }
    }

    private suspend fun loadFromLocal(sessionUserId: String) {
        try {
            trustedPlaceDao.getTrustedPlacesFlow(sessionUserId).collect { entities ->
                if (currentUserId == sessionUserId) {
                    val domainPlaces = entities.map { it.toDomainModel() }
                    _trustedPlaces.value = domainPlaces
                    geofenceManager.updateGeofences(domainPlaces.filter { it.isEnabled })
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("TrustedPlacesService", "Failed to load local trusted places", e)
        }
    }

    private suspend fun syncFromCloud(sessionUserId: String) {
        if (firestore == null || sessionUserId.isBlank()) return
        try {
            val snapshot = firestore.collection("users").document(sessionUserId)
                .collection("trusted_places").get().await()
                
            val places = snapshot.documents.mapNotNull { doc ->
                try {
                    TrustedPlace.fromMap(doc.data ?: emptyMap())
                } catch (e: Exception) {
                    null
                }
            }
            
            if (currentUserId == sessionUserId) {
                trustedPlaceDao.insertTrustedPlaces(places.map { it.toEntity() })
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("TrustedPlacesService", "Failed to sync trusted places from cloud", e)
        }
    }

    suspend fun addTrustedPlace(place: TrustedPlace) {
        val newPlace = place.copy(
            placeId = if (place.placeId.isNotBlank()) place.placeId else UUID.randomUUID().toString(),
            userId = if (place.userId.isNotBlank()) place.userId else currentUserId,
            createdDate = if (place.createdDate != 0L) place.createdDate else System.currentTimeMillis(),
            lastUpdated = System.currentTimeMillis()
        )
        try {
            trustedPlaceDao.insertTrustedPlace(newPlace.toEntity())
            firestore?.collection("users")?.document(currentUserId)
                ?.collection("trusted_places")?.document(newPlace.placeId)?.set(newPlace.toMap())
        } catch (e: Exception) {
            Log.e("TrustedPlacesService", "Failed to add trusted place", e)
        }
    }
    
    suspend fun updateTrustedPlace(place: TrustedPlace) {
        val updatedPlace = place.copy(
            userId = if (place.userId.isNotBlank()) place.userId else currentUserId,
            lastUpdated = System.currentTimeMillis()
        )
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

    suspend fun clearSession(userId: String) {
        sessionJob?.cancel()
        sessionJob = null

        val targetUid = userId.ifBlank { currentUserId }
        if (targetUid.isNotBlank()) {
            try {
                trustedPlaceDao.deleteTrustedPlacesForUser(targetUid)
            } catch (e: Exception) {
                Log.e("TrustedPlacesService", "Failed to clear local trusted places for $targetUid: ${e.message}")
            }
        }
        currentUserId = ""
        _trustedPlaces.value = emptyList()
        try {
            geofenceManager.updateGeofences(emptyList())
        } catch (e: Exception) {
            // Ignore geofence reset exceptions
        }
    }
}
