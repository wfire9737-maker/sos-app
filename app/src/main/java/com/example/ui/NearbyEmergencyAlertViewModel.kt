package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.NearbyEmergencyAlert
import com.example.repository.NearbyEmergencyAlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NearbyEmergencyAlertViewModel @Inject constructor(
    private val repository: NearbyEmergencyAlertRepository
) : ViewModel() {

    val activeEmergencyAlerts: StateFlow<List<NearbyEmergencyAlert>> = repository.activeEmergencyAlerts
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    fun dismissAlert(alertId: String) {
        repository.dismissAlert(alertId)
    }
}
