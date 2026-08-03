package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.TrustedPlace
import com.example.ui.GuardianViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTrustedPlaceScreen(
    viewModel: GuardianViewModel,
    placeId: String?,
    onNavigateBack: () -> Unit
) {
    val trustedPlaces by viewModel.trustedPlaces.collectAsState()
    val existingPlace = trustedPlaces.find { it.placeId == placeId }
    
    var name by remember { mutableStateOf(existingPlace?.name ?: "") }
    var address by remember { mutableStateOf(existingPlace?.address ?: "") }
    var latitude by remember { mutableStateOf(existingPlace?.latitude?.toString() ?: "") }
    var longitude by remember { mutableStateOf(existingPlace?.longitude?.toString() ?: "") }
    var radius by remember { mutableStateOf(existingPlace?.radius?.toFloat() ?: 100f) }
    
    var alwaysSendSos by remember { mutableStateOf(existingPlace?.alwaysSendSos ?: true) }
    var reduceNotificationSound by remember { mutableStateOf(existingPlace?.reduceNotificationSound ?: false) }
    var skipAutomaticPhoneCall by remember { mutableStateOf(existingPlace?.skipAutomaticPhoneCall ?: false) }
    var delaySosSeconds by remember { mutableStateOf(existingPlace?.delaySosSeconds ?: 0) }
    var showConfirmationDialog by remember { mutableStateOf(existingPlace?.showConfirmationDialog ?: false) }

    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (placeId == null) "Add Trusted Place" else "Edit Trusted Place") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Place Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address / Location Search") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(onClick = {
                coroutineScope.launch {
                    val result = viewModel.searchCoordinates(address)
                    if (result != null) {
                        latitude = result.first.toString()
                        longitude = result.second.toString()
                    }
                }
            }) {
                Text("Search Address")
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Latitude") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Button(
                onClick = {
                    coroutineScope.launch {
                        val loc = viewModel.getCurrentLocationOnce()
                        if (loc != null) {
                            latitude = loc.latitude.toString()
                            longitude = loc.longitude.toString()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Current Location")
            }
            
            val latDouble = latitude.toDoubleOrNull() ?: 0.0
            val lngDouble = longitude.toDoubleOrNull() ?: 0.0
            val camPos = com.google.maps.android.compose.rememberCameraPositionState {
                position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(com.google.android.gms.maps.model.LatLng(latDouble, lngDouble), 15f)
            }
            LaunchedEffect(latDouble, lngDouble) {
                camPos.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(com.google.android.gms.maps.model.LatLng(latDouble, lngDouble), 15f)
            }
            com.google.maps.android.compose.GoogleMap(modifier = Modifier.fillMaxWidth().height(200.dp), cameraPositionState = camPos) {
                com.google.maps.android.compose.Circle(center = com.google.android.gms.maps.model.LatLng(latDouble, lngDouble), radius = radius.toDouble(), strokeColor = MaterialTheme.colorScheme.primary, fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            }
            Text("Radius: ${radius.toInt()} meters")
            Slider(
                value = radius,
                onValueChange = { radius = it },
                valueRange = 50f..1000f,
                steps = 19
            )
            
            HorizontalDivider()
            
            Text("SOS Behavior Settings", style = MaterialTheme.typography.titleMedium)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = alwaysSendSos, onCheckedChange = { alwaysSendSos = it })
                Text("Always send SOS")
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = reduceNotificationSound, onCheckedChange = { reduceNotificationSound = it })
                Text("Reduce notification sound")
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = skipAutomaticPhoneCall, onCheckedChange = { skipAutomaticPhoneCall = it })
                Text("Skip automatic phone call")
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = showConfirmationDialog, onCheckedChange = { showConfirmationDialog = it })
                Text("Show confirmation dialog")
            }
            
            Button(
                onClick = {
                    val lat = latitude.toDoubleOrNull() ?: 0.0
                    val lng = longitude.toDoubleOrNull() ?: 0.0
                    val place = TrustedPlace(
                        placeId = placeId ?: "",
                        name = name,
                        address = address,
                        latitude = lat,
                        longitude = lng,
                        radius = radius.toDouble(),
                        alwaysSendSos = alwaysSendSos,
                        reduceNotificationSound = reduceNotificationSound,
                        skipAutomaticPhoneCall = skipAutomaticPhoneCall,
                        delaySosSeconds = delaySosSeconds,
                        showConfirmationDialog = showConfirmationDialog
                    )
                    if (placeId == null) {
                        viewModel.addTrustedPlace(place)
                    } else {
                        viewModel.updateTrustedPlace(place)
                    }
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (placeId == null) "Add Place" else "Save Changes")
            }
        }
    }
}
