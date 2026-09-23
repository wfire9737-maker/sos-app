package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ble.nearby.NearbyBleManager
import com.example.ble.nearby.NearbyConnectionState
import com.example.model.NearbyChatMessage
import com.example.repository.NearbyChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NearbyChatViewModel @Inject constructor(
    private val repository: NearbyChatRepository,
    private val nearbyBleManager: NearbyBleManager
) : ViewModel() {
    
    val messages: StateFlow<List<NearbyChatMessage>> = repository.messages

    fun getConnectionState(macAddress: String): StateFlow<NearbyConnectionState> {
        return nearbyBleManager.nearbyDevices.map { devices ->
            devices[macAddress]?.connectionState ?: NearbyConnectionState.DISCONNECTED
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NearbyConnectionState.DISCONNECTED)
    }

    fun sendText(macAddress: String, text: String): Boolean {
        return repository.sendText(macAddress, text)
    }
}
