package com.example.service

import android.content.Context
import com.example.model.NotificationModel
import com.example.model.NotificationCategory
import com.example.model.NotificationItem
import com.example.model.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationProvider(
    private val context: Context,
    private val notificationService: NotificationService
) {
    private val _notifications = MutableStateFlow<List<NotificationModel>>(emptyList())
    val notifications: StateFlow<List<NotificationModel>> = _notifications.asStateFlow()

    init {
        CoroutineScope(Dispatchers.Default).launch {
            notificationService.notifications.collect { items ->
                _notifications.value = items.map { mapItemToModel(it) }
            }
        }
    }

    fun getNotifications(): List<NotificationModel> {
        return notifications.value
    }

    fun addNotification(item: NotificationModel) {
        // Map NotificationModel to NotificationItem for backend service persistence
        val itemLegacy = mapModelToItem(item)
        notificationService.addNotification(itemLegacy)
    }

    fun deleteNotification(id: String) {
        notificationService.deleteNotification(id)
    }

    fun markAsRead(id: String) {
        notificationService.markAsRead(id)
    }

    fun markAllAsRead() {
        notificationService.markAllAsRead()
    }

    fun triggerFCMNotification(category: NotificationCategory) {
        val typeLegacy = mapCategoryToType(category)
        notificationService.triggerSimulatedFCMNotification(typeLegacy)
    }

    companion object {
        fun mapItemToModel(item: NotificationItem): NotificationModel {
            val category = when (item.type) {
                NotificationType.EMERGENCY -> NotificationCategory.EMERGENCY_ALERTS
                NotificationType.BATTERY_LOW -> NotificationCategory.BATTERY_ALERTS
                NotificationType.DEVICE_OFFLINE -> NotificationCategory.DEVICE_DISCONNECTED
                NotificationType.GPS_UNAVAILABLE -> NotificationCategory.GPS_UNAVAILABLE
                NotificationType.EMERGENCY_CANCELLED -> NotificationCategory.EMERGENCY_RESOLVED
                NotificationType.SAFE_ARRIVAL -> NotificationCategory.SAFE_ARRIVAL
                NotificationType.FIRMWARE_UPDATE -> NotificationCategory.FIRMWARE_UPDATES
            }
            val severity = when (category) {
                NotificationCategory.EMERGENCY_ALERTS -> "CRITICAL"
                NotificationCategory.BATTERY_ALERTS,
                NotificationCategory.DEVICE_DISCONNECTED,
                NotificationCategory.GPS_UNAVAILABLE -> "WARNING"
                else -> "INFO"
            }
            return NotificationModel(
                id = item.id,
                title = item.title,
                body = item.body,
                timestamp = item.timestamp,
                category = category,
                isRead = item.isRead,
                deviceId = item.deviceId,
                severity = severity
            )
        }

        fun mapModelToItem(model: NotificationModel): NotificationItem {
            val type = mapCategoryToType(model.category)
            return NotificationItem(
                id = model.id,
                title = model.title,
                body = model.body,
                timestamp = model.timestamp,
                type = type,
                isRead = model.isRead,
                deviceId = model.deviceId
            )
        }

        private fun mapCategoryToType(category: NotificationCategory): NotificationType {
            return when (category) {
                NotificationCategory.EMERGENCY_ALERTS -> NotificationType.EMERGENCY
                NotificationCategory.BATTERY_ALERTS -> NotificationType.BATTERY_LOW
                NotificationCategory.DEVICE_DISCONNECTED -> NotificationType.DEVICE_OFFLINE
                NotificationCategory.GPS_UNAVAILABLE -> NotificationType.GPS_UNAVAILABLE
                NotificationCategory.EMERGENCY_RESOLVED -> NotificationType.EMERGENCY_CANCELLED
                NotificationCategory.SAFE_ARRIVAL -> NotificationType.SAFE_ARRIVAL
                NotificationCategory.FIRMWARE_UPDATES -> NotificationType.FIRMWARE_UPDATE
            }
        }
    }
}
