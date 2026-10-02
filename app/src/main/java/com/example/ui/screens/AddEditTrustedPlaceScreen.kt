package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.TrustedPlace
import com.example.ui.GuardianViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTrustedPlaceScreen(
    viewModel: GuardianViewModel,
    placeId: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
    var skipAutomaticSms by remember { mutableStateOf(existingPlace?.skipAutomaticSms ?: false) }
    var delaySosSeconds by remember { mutableStateOf(existingPlace?.delaySosSeconds ?: 0) }
    var showConfirmationDialog by remember { mutableStateOf(existingPlace?.showConfirmationDialog ?: false) }
    var isEnabled by remember { mutableStateOf(existingPlace?.isEnabled ?: true) }

    // Real-time GPS location detection state
    var isFetchingLocation by remember { mutableStateOf(false) }
    var locationErrorMessage by remember { mutableStateOf<String?>(null) }
    var detectedAccuracy by remember { mutableStateOf<Float?>(null) }

    val fetchLocation: () -> Unit = {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            // Permission missing: request via launcher
            locationErrorMessage = null
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        if (!isGpsEnabled && !isNetworkEnabled) {
            locationErrorMessage = "Location services are disabled. Please enable GPS in device settings."
            Toast.makeText(context, "Location services are turned off", Toast.LENGTH_SHORT).show()
            try {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } catch (e: Exception) {
                // Ignore if intent cannot be resolved directly
            }
        } else {
            isFetchingLocation = true
            locationErrorMessage = null
            coroutineScope.launch {
                try {
                    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                    val freshLoc: Location? = try {
                        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                    } catch (e: Exception) {
                        null
                    } ?: viewModel.getCurrentLocationOnce()

                    if (freshLoc == null) {
                        locationErrorMessage = "Unable to obtain fresh GPS fix. Please ensure you are outdoors or near a window and try again."
                    } else {
                        val acc = if (freshLoc.hasAccuracy()) freshLoc.accuracy else 999f
                        if (acc > 50f) {
                            locationErrorMessage = "Location accuracy is too low (${acc.toInt()}m). Please move to an open area and try again."
                        } else {
                            detectedAccuracy = acc
                            latitude = freshLoc.latitude.toString()
                            longitude = freshLoc.longitude.toString()
                            locationErrorMessage = null

                            // Reverse geocode to readable address
                            val geocoded = withContext(Dispatchers.IO) {
                                try {
                                    val geocoder = Geocoder(context, Locale.getDefault())
                                    val list = geocoder.getFromLocation(freshLoc.latitude, freshLoc.longitude, 1)
                                    if (!list.isNullOrEmpty()) list[0].getAddressLine(0) else null
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            if (!geocoded.isNullOrBlank()) {
                                address = geocoded
                            } else if (address.isBlank()) {
                                address = "Current location detected"
                            }
                            Toast.makeText(context, "Location captured (±${acc.toInt()}m)", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    locationErrorMessage = "Failed to fetch location: ${e.message ?: "Unknown error"}"
                } finally {
                    isFetchingLocation = false
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchLocation()
        } else {
            locationErrorMessage = "Location permission is required to detect your current position for Trusted Places."
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val triggerLocationCapture = {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            fetchLocation()
        }
    }

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
                placeholder = { Text("e.g., Home, Office, Gym") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Location Section
            val latDouble = latitude.toDoubleOrNull()
            val lngDouble = longitude.toDoubleOrNull()
            val hasValidLocation = latDouble != null && lngDouble != null && (latDouble != 0.0 || lngDouble != 0.0)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Current Location",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isFetchingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    if (hasValidLocation && latDouble != null && lngDouble != null) {
                        // Location is captured/preserved
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (placeId == null) "Current location detected" else "Saved Trusted Place location",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = if (address.isNotBlank()) address else "Current location detected",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            if (detectedAccuracy != null) {
                                Text(
                                    text = "Accuracy: ±${detectedAccuracy?.toInt()} m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = String.format(Locale.US, "Latitude: %.5f", latDouble),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(Locale.US, "Longitude: %.5f", lngDouble),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = triggerLocationCapture,
                            enabled = !isFetchingLocation,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update Location")
                        }
                    } else {
                        // No location captured yet (New place)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "You must physically be at the location to add this Trusted Place.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = triggerLocationCapture,
                            enabled = !isFetchingLocation,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Use Current Location")
                        }
                    }

                    if (!locationErrorMessage.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = locationErrorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Radius Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Geofence Radius", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text("${radius.toInt()} meters", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 50f..1000f,
                    steps = 19
                )
            }

            // Enable / Disable Switch Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable this Trusted Place",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isEnabled) "Active in location matching & geofencing" else "Temporarily inactive (settings preserved)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }
            }

            HorizontalDivider()

            Text("SOS Behavior Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = alwaysSendSos, onCheckedChange = { alwaysSendSos = it })
                Text("Always send SOS", modifier = Modifier.padding(start = 8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = reduceNotificationSound, onCheckedChange = { reduceNotificationSound = it })
                Text("Reduce notification sound", modifier = Modifier.padding(start = 8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = skipAutomaticPhoneCall, onCheckedChange = { skipAutomaticPhoneCall = it })
                Text("Skip automatic phone call", modifier = Modifier.padding(start = 8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = skipAutomaticSms, onCheckedChange = { skipAutomaticSms = it })
                Text("Skip automatic SMS", modifier = Modifier.padding(start = 8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = showConfirmationDialog, onCheckedChange = { showConfirmationDialog = it })
                Text("Show confirmation dialog", modifier = Modifier.padding(start = 8.dp))
            }

            Button(
                onClick = {
                    if (!hasValidLocation || latDouble == null || lngDouble == null) {
                        Toast.makeText(context, "Please capture your current location before saving", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val lat = latDouble
                    val lng = lngDouble
                    val place = existingPlace?.copy(
                        name = name.ifBlank { "Trusted Place" },
                        address = address.ifBlank { "Current location detected" },
                        latitude = lat,
                        longitude = lng,
                        radius = radius.toDouble(),
                        alwaysSendSos = alwaysSendSos,
                        reduceNotificationSound = reduceNotificationSound,
                        skipAutomaticPhoneCall = skipAutomaticPhoneCall,
                        skipAutomaticSms = skipAutomaticSms,
                        delaySosSeconds = delaySosSeconds,
                        showConfirmationDialog = showConfirmationDialog,
                        isEnabled = isEnabled,
                        lastUpdated = System.currentTimeMillis()
                    ) ?: TrustedPlace(
                        placeId = placeId ?: "",
                        userId = "",
                        name = name.ifBlank { "Trusted Place" },
                        address = address.ifBlank { "Current location detected" },
                        latitude = lat,
                        longitude = lng,
                        radius = radius.toDouble(),
                        createdDate = System.currentTimeMillis(),
                        lastUpdated = System.currentTimeMillis(),
                        alwaysSendSos = alwaysSendSos,
                        reduceNotificationSound = reduceNotificationSound,
                        skipAutomaticPhoneCall = skipAutomaticPhoneCall,
                        skipAutomaticSms = skipAutomaticSms,
                        delaySosSeconds = delaySosSeconds,
                        showConfirmationDialog = showConfirmationDialog,
                        isEnabled = isEnabled
                    )
                    if (placeId == null) {
                        viewModel.addTrustedPlace(place)
                    } else {
                        viewModel.updateTrustedPlace(place)
                    }
                    onNavigateBack()
                },
                enabled = hasValidLocation && name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (placeId == null) "Add Place" else "Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
