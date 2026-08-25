package com.example.ble

import java.util.UUID

object BleProtocol {
    const val DEVICE_NAME = "Physical-SOS-ESP32"
    val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    val STATUS_CHARACTERISTIC_UUID: UUID = UUID.fromString("beb54803-36e1-4688-b7f5-ea07361b26a8")
    val BATTERY_CHARACTERISTIC_UUID: UUID = UUID.fromString("beb54804-36e1-4688-b7f5-ea07361b26a8")
    val GPS_CHARACTERISTIC_UUID: UUID = UUID.fromString("beb54805-36e1-4688-b7f5-ea07361b26a8")
    val MPU6050_CHARACTERISTIC_UUID: UUID = UUID.fromString("beb54806-36e1-4688-b7f5-ea07361b26a8")
    val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    
    val CMD_RESET_SOS = byteArrayOf(0x01)
}

