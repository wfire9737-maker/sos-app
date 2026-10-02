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
    val skipAutomaticSms: Boolean = false,
    val delaySosSeconds: Int,
    val showConfirmationDialog: Boolean,
    val isEnabled: Boolean = true
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
        skipAutomaticSms = skipAutomaticSms,
        delaySosSeconds = delaySosSeconds,
        showConfirmationDialog = showConfirmationDialog,
        isEnabled = isEnabled
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
        skipAutomaticSms = skipAutomaticSms,
        delaySosSeconds = delaySosSeconds,
        showConfirmationDialog = showConfirmationDialog,
        isEnabled = isEnabled
    )
}
