package com.example.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

fun Context.hasFineLocationPermission(): Boolean {
    return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
}

fun Context.hasCoarseLocationPermission(): Boolean {
    return hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
}

fun Context.hasBackgroundLocationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        true
    }
}

fun Context.hasCallPhonePermission(): Boolean {
    return hasPermission(Manifest.permission.CALL_PHONE)
}

fun Context.hasSendSmsPermission(): Boolean {
    return hasPermission(Manifest.permission.SEND_SMS)
}

fun Context.hasReadContactsPermission(): Boolean {
    return hasPermission(Manifest.permission.READ_CONTACTS)
}

fun Context.hasBluetoothScanPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        hasPermission(Manifest.permission.BLUETOOTH_SCAN)
    } else {
        true
    }
}

fun Context.hasBluetoothConnectPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        true
    }
}

fun Context.hasBluetoothAdvertisePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    } else {
        true
    }
}

fun Context.hasPostNotificationsPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        true
    }
}

fun Context.hasMicrophonePermission(): Boolean {
    return hasPermission(Manifest.permission.RECORD_AUDIO)
}
