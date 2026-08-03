package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.TrustedPlace

@Entity(tableName = "trusted_places")
data class TrustedPlaceEntity(
    @PrimaryKey
    val placeId: String,
    val userId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
    val createdDate: Long,
    val lastUpdated: Long,
    val alwaysSendSos: Boolean,
    val reduceNotificationSound: Boolean,
    val skipAutomaticPhoneCall: Boolean,
    val delaySosSeconds: Int,
    val showConfirmationDialog: Boolean
)

fun TrustedPlace.toEntity(): TrustedPlaceEntity {
    return TrustedPlaceEntity(
        placeId = placeId,
        userId = userId,
        name = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
        radius = radius,
        createdDate = createdDate,
        lastUpdated = lastUpdated,
        alwaysSendSos = alwaysSendSos,
        reduceNotificationSound = reduceNotificationSound,
        skipAutomaticPhoneCall = skipAutomaticPhoneCall,
        delaySosSeconds = delaySosSeconds,
        showConfirmationDialog = showConfirmationDialog
    )
}

fun TrustedPlaceEntity.toDomainModel(): TrustedPlace {
    return TrustedPlace(
        placeId = placeId,
        userId = userId,
        name = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
        radius = radius,
        createdDate = createdDate,
        lastUpdated = lastUpdated,
        alwaysSendSos = alwaysSendSos,
        reduceNotificationSound = reduceNotificationSound,
        skipAutomaticPhoneCall = skipAutomaticPhoneCall,
        delaySosSeconds = delaySosSeconds,
        showConfirmationDialog = showConfirmationDialog
    )
}
