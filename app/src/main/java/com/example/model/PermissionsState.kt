package com.example.model

data class PermissionsState(
    val locationGranted: Boolean = false,
    val backgroundLocationGranted: Boolean = false,
    val callsGranted: Boolean = false,
    val smsGranted: Boolean = false,
    val contactsGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val audioGranted: Boolean = false,
    val overlayGranted: Boolean = false
)
