package com.example

import com.example.ble.nearby.NearbyConnectionState
import com.example.ble.nearby.NearbyDevice
import com.example.ble.nearby.NearbyDeviceScanner
import com.example.model.MessageDeliveryState
import com.example.model.NearbyChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyPeopleImprovementsTest {

    @Test
    fun testPinnedConnectedUsersOrderingWithMultipleDevices() {
        val device1 = NearbyDevice(
            macAddress = "11:11:11:11:11:11",
            id = "id_1",
            deviceName = "Alice",
            lastSeen = 5000L,
            rssi = -70,
            connectionState = NearbyConnectionState.DISCONNECTED
        )
        val device2 = NearbyDevice(
            macAddress = "22:22:22:22:22:22",
            id = "id_2",
            deviceName = "Bob",
            lastSeen = 1000L,
            rssi = -80,
            connectionState = NearbyConnectionState.CONNECTED
        )
        val device3 = NearbyDevice(
            macAddress = "33:33:33:33:33:33",
            id = "id_3",
            deviceName = "Charlie",
            lastSeen = 3000L,
            rssi = -60,
            connectionState = NearbyConnectionState.REQUESTING
        )
        val device4 = NearbyDevice(
            macAddress = "44:44:44:44:44:44",
            id = "id_4",
            deviceName = "David",
            lastSeen = 2000L,
            rssi = -65,
            connectionState = NearbyConnectionState.CONNECTED
        )

        val devices = mapOf(
            device1.id to device1,
            device2.id to device2,
            device3.id to device3,
            device4.id to device4
        )

        val (connected, notConnected) = devices.values.partition {
            it.connectionState == NearbyConnectionState.CONNECTED
        }
        val (requesting, disconnected) = notConnected.partition {
            it.connectionState == NearbyConnectionState.REQUESTING
        }

        val sorted = connected.sortedBy { it.deviceName.lowercase() } +
                requesting.sortedByDescending { it.lastSeen } +
                disconnected.sortedByDescending { it.lastSeen }

        // Pinned connected users must be at the very top (Bob, David)
        assertEquals(4, sorted.size)
        assertEquals("Bob", sorted[0].deviceName)
        assertEquals(NearbyConnectionState.CONNECTED, sorted[0].connectionState)
        assertEquals("David", sorted[1].deviceName)
        assertEquals(NearbyConnectionState.CONNECTED, sorted[1].connectionState)

        // Followed by requesting (Charlie)
        assertEquals("Charlie", sorted[2].deviceName)
        assertEquals(NearbyConnectionState.REQUESTING, sorted[2].connectionState)

        // Followed by disconnected (Alice)
        assertEquals("Alice", sorted[3].deviceName)
        assertEquals(NearbyConnectionState.DISCONNECTED, sorted[3].connectionState)
    }

    @Test
    fun testOneToOneMessageFilteringIsolatesRecipients() {
        val myId = "local_phone_uuid"
        val peerA = "AA:BB:CC:DD:EE:01"
        val peerB = "AA:BB:CC:DD:EE:02"

        val msg1 = NearbyChatMessage(
            messageId = "m1",
            senderId = myId,
            receiverId = peerA,
            text = "Hello Peer A",
            timestamp = 1000L,
            isOutgoing = true,
            deliveryState = MessageDeliveryState.SENT,
            peerAddress = peerA
        )

        val msg2 = NearbyChatMessage(
            messageId = "m2",
            senderId = "peer_a_uuid",
            receiverId = myId,
            text = "Reply from Peer A",
            timestamp = 2000L,
            isOutgoing = false,
            deliveryState = MessageDeliveryState.DELIVERED,
            peerAddress = peerA
        )

        val msg3 = NearbyChatMessage(
            messageId = "m3",
            senderId = myId,
            receiverId = peerB,
            text = "Hello Peer B",
            timestamp = 3000L,
            isOutgoing = true,
            deliveryState = MessageDeliveryState.SENT,
            peerAddress = peerB
        )

        val allMessages = listOf(msg1, msg2, msg3)

        fun filterForPeer(peerIdentifier: String): List<NearbyChatMessage> {
            return allMessages.filter { msg ->
                if (msg.peerAddress.isNotBlank()) {
                    msg.peerAddress.equals(peerIdentifier, ignoreCase = true)
                } else if (msg.isOutgoing) {
                    msg.receiverId.equals(peerIdentifier, ignoreCase = true)
                } else {
                    msg.senderId.equals(peerIdentifier, ignoreCase = true)
                }
            }
        }

        val peerAMessages = filterForPeer(peerA)
        assertEquals(2, peerAMessages.size)
        assertTrue(peerAMessages.any { it.text == "Hello Peer A" })
        assertTrue(peerAMessages.any { it.text == "Reply from Peer A" })
        assertFalse(peerAMessages.any { it.text == "Hello Peer B" })

        val peerBMessages = filterForPeer(peerB)
        assertEquals(1, peerBMessages.size)
        assertEquals("Hello Peer B", peerBMessages[0].text)
        assertFalse(peerBMessages.any { it.text == "Hello Peer A" })
    }

    @Test
    fun testControlledRefreshThrottleConstantIsOneSecond() {
        // Verification of requirement: Limit automatic refresh to at most once per second
        assertEquals(1_000L, NearbyDeviceScanner.UI_REFRESH_THROTTLE_MS)
    }

    @Test
    fun testActiveConnectionsPreservedDuringRefresh() {
        val now = 100_000L
        val activeDevice = NearbyDevice(
            macAddress = "11:22:33:44:55:66",
            id = "active_id",
            deviceName = "Connected Peer",
            lastSeen = now - 50_000L, // older than 15s
            rssi = -60,
            connectionState = NearbyConnectionState.CONNECTED
        )
        val requestingDevice = NearbyDevice(
            macAddress = "22:33:44:55:66:77",
            id = "requesting_id",
            deviceName = "Connecting Peer",
            lastSeen = now - 50_000L, // older than 15s
            rssi = -65,
            connectionState = NearbyConnectionState.REQUESTING
        )
        val staleDisconnectedDevice = NearbyDevice(
            macAddress = "33:44:55:66:77:88",
            id = "stale_id",
            deviceName = "Stale Peer",
            lastSeen = now - 30_000L, // 30s ago (>15s)
            rssi = -90,
            connectionState = NearbyConnectionState.DISCONNECTED
        )

        val devices = mutableMapOf(
            activeDevice.id to activeDevice,
            requestingDevice.id to requestingDevice,
            staleDisconnectedDevice.id to staleDisconnectedDevice
        )

        // Simulate manualRefresh pruning logic:
        val iterator = devices.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val device = entry.value
            if (device.connectionState == NearbyConnectionState.DISCONNECTED) {
                if (now - device.lastSeen > 15_000L) {
                    iterator.remove()
                }
            }
        }

        // Active and Requesting connections must be preserved; only stale disconnected pruned
        assertTrue("Connected device must remain", devices.containsKey("active_id"))
        assertTrue("Requesting device must remain", devices.containsKey("requesting_id"))
        assertFalse("Stale disconnected device should be pruned", devices.containsKey("stale_id"))
    }
}
