package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.TrustedPlace
import com.example.ui.GuardianViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustedPlacesScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddPlace: () -> Unit,
    onNavigateToEditPlace: (String) -> Unit
) {
    val trustedPlaces by viewModel.trustedPlaces.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trusted Places") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddPlace) {
                Icon(Icons.Default.Add, contentDescription = "Add Trusted Place")
            }
        }
    ) { padding ->
        if (trustedPlaces.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No Trusted Places yet. Add one to customize SOS behavior.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(trustedPlaces) { place ->
                    TrustedPlaceItem(
                        place = place,
                        onEdit = { onNavigateToEditPlace(place.placeId) },
                        onDelete = {
                            viewModel.deleteTrustedPlace(place.placeId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TrustedPlaceItem(
    place: TrustedPlace,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = place.name, style = MaterialTheme.typography.titleMedium)
            Text(text = place.address, style = MaterialTheme.typography.bodyMedium)
            val camPos = com.google.maps.android.compose.rememberCameraPositionState { position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(com.google.android.gms.maps.model.LatLng(place.latitude, place.longitude), 15f) }
            com.google.maps.android.compose.GoogleMap(modifier = Modifier.fillMaxWidth().height(150.dp), cameraPositionState = camPos) {
                com.google.maps.android.compose.Circle(center = com.google.android.gms.maps.model.LatLng(place.latitude, place.longitude), radius = place.radius, strokeColor = MaterialTheme.colorScheme.primary, fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            }
            Text(text = "Radius: ${place.radius}m", style = MaterialTheme.typography.bodySmall)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
