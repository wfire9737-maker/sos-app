package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FavoritePlace
import com.example.model.UserLocation
import com.example.ui.GuardianViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val locationState by viewModel.currentLocation.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()
    val isTracking by viewModel.isTrackingLocation.collectAsState()
    val isSimulating by viewModel.isLocationSimulation.collectAsState()

    // Automatically boot location tracking when Map opens if not already running
    LaunchedEffect(Unit) {
        if (!isTracking) {
            viewModel.startLocationTracking()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var zoomLevel by remember { mutableStateOf(15f) } // Simulated zoom scale (10f to 20f)
    var trafficEnabled by remember { mutableStateOf(locationState.trafficEnabled) }
    var viewMode by remember { mutableStateOf(locationState.viewMode) } // NORMAL, SATELLITE, TERRAIN

    // Manual camera pan offsets
    var cameraOffsetLat by remember { mutableStateOf(0.0) }
    var cameraOffsetLng by remember { mutableStateOf(0.0) }
    var isCameraPanned by remember { mutableStateOf(false) }

    // Dialog state
    var showAddFavoriteDialog by remember { mutableStateOf(false) }
    var favPlaceName by remember { mutableStateOf("") }
    var favPlaceType by remember { mutableStateOf("OTHER") } // HOME, WORK, COLLEGE, OTHER

    val currentLat = locationState.latitude + cameraOffsetLat
    val currentLng = locationState.longitude + cameraOffsetLng

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "GPS Tracking & Live Map",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("map_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                },
                actions = {
                    // Start / Stop tracking quick action
                    IconButton(
                        onClick = {
                            if (isTracking) {
                                viewModel.stopLocationTracking()
                            } else {
                                viewModel.startLocationTracking()
                            }
                        },
                        modifier = Modifier.testTag("map_toggle_tracking")
                    ) {
                        Icon(
                            imageVector = if (isTracking) Icons.Default.LocationOn else Icons.Default.LocationOff,
                            contentDescription = "Toggle GPS",
                            tint = if (isTracking) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- TOP SEARCH & QUICK ACTION PANEL ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search places (e.g. Golden Gate, Safeway)", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("map_search_input")
                )

                Button(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            viewModel.searchLocation(searchQuery)
                            cameraOffsetLat = 0.0
                            cameraOffsetLng = 0.0
                            isCameraPanned = false
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("map_search_btn")
                ) {
                    Text("Go", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // --- THE MAP ENGINE AND TELEMETRY GRID ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .testTag("map_container")
            ) {
                // Interactive Custom Painted Map Canvas
                MapCanvasEngine(
                    centerLat = currentLat,
                    centerLng = currentLng,
                    userLat = locationState.latitude,
                    userLng = locationState.longitude,
                    zoom = zoomLevel,
                    bearing = locationState.bearing,
                    route = routePoints,
                    favorites = locationState.favorites,
                    viewMode = viewMode,
                    trafficEnabled = trafficEnabled,
                    onDrag = { dx, dy ->
                        // Convert touch drag deltas to relative Lat/Lng movements
                        val scaling = 0.000002 * (21f - zoomLevel)
                        cameraOffsetLat -= dy * scaling
                        cameraOffsetLng += dx * scaling
                        isCameraPanned = true
                    }
                )

                // --- FLOATING ZOOM & UTILITY HUD (Left column) ---
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Zoom In
                    FloatingMapButton(icon = Icons.Default.Add, tag = "zoom_in") {
                        if (zoomLevel < 20f) zoomLevel += 0.5f
                    }
                    // Zoom Out
                    FloatingMapButton(icon = Icons.Default.Remove, tag = "zoom_out") {
                        if (zoomLevel > 11f) zoomLevel -= 0.5f
                    }

                    // View Type Toggle
                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        FloatingMapButton(icon = Icons.Default.Layers, tag = "map_layers_btn") {
                            expanded = true
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Standard Normal Map", fontSize = 12.sp) },
                                leadingIcon = { Text("🗺️") },
                                onClick = {
                                    viewMode = "NORMAL"
                                    viewModel.updateMapOptions("NORMAL", trafficEnabled)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Satellite Photo Layer", fontSize = 12.sp) },
                                leadingIcon = { Text("🛰️") },
                                onClick = {
                                    viewMode = "SATELLITE"
                                    viewModel.updateMapOptions("SATELLITE", trafficEnabled)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Terrain Topo Grid", fontSize = 12.sp) },
                                leadingIcon = { Text("⛰️") },
                                onClick = {
                                    viewMode = "TERRAIN"
                                    viewModel.updateMapOptions("TERRAIN", trafficEnabled)
                                    expanded = false
                                }
                            )
                        }
                    }

                    // Traffic Toggle
                    FloatingMapButton(
                        icon = Icons.Default.Traffic,
                        tag = "traffic_toggle",
                        activeColor = if (trafficEnabled) Color(0xFFFF5722) else Color.White,
                        iconTint = if (trafficEnabled) Color.White else Color(0xFF44474E)
                    ) {
                        trafficEnabled = !trafficEnabled
                        viewModel.updateMapOptions(viewMode, trafficEnabled)
                    }
                }

                // --- COMPASS, RECENTER & FAVORITES TRIGGER (Right column) ---
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Compass (Rotates with Bearing)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E7F0), CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Compass",
                            tint = Color(0xFF0061A4),
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(locationState.bearing)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp)
                        ) {
                            Text(
                                "N",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }
                    }

                    // Recenter Camera
                    if (isCameraPanned) {
                        FloatingMapButton(
                            icon = Icons.Default.MyLocation,
                            tag = "recenter_btn",
                            activeColor = MaterialTheme.colorScheme.primary,
                            iconTint = Color.White
                        ) {
                            cameraOffsetLat = 0.0
                            cameraOffsetLng = 0.0
                            isCameraPanned = false
                        }
                    }

                    // Add Favorite Place
                    FloatingMapButton(
                        icon = Icons.Default.Star,
                        tag = "add_favorite_spot_btn",
                        activeColor = Color(0xFFFFC107),
                        iconTint = Color.DarkGray
                    ) {
                        favPlaceName = ""
                        favPlaceType = "OTHER"
                        showAddFavoriteDialog = true
                    }
                }

                // --- LIVE FLIGHT HUD INSTRUMENT DECK (Bottom overlay inside map) ---
                TelemetryInstrumentHUD(
                    location = locationState,
                    onResetOdometer = { viewModel.resetDistance() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // --- BOTTOM DRAWER CONTROL STATION ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(16.dp)
            ) {
                // Tracking Status Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isTracking) Color(0xFF4CAF50) else Color(0xFFF44336))
                            )
                            Text(
                                text = if (isTracking) "ACTIVE REAL-TIME GPS MONITOR" else "GPS RADIO IDLE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTracking) Color(0xFF2E7D32) else Color(0xFFC62828),
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = if (isTracking) "Pushing coordinates to cloud every 3s" else "Telemetry feed is paused",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Mode switch (Real vs Simulated)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E7F0), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .clickable { viewModel.toggleLocationSimulation(!isSimulating) }
                            .testTag("mode_sim_toggle")
                    ) {
                        Text(
                            text = if (isSimulating) "🤖 Simulated Track" else "📱 Device GPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSimulating) Color(0xFF0061A4) else Color(0xFF4F5B66)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(8.dp))

                // Favorite Places Quick Links
                Text(
                    text = "FAVORITE SECURE LOCATIONS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    locationState.favorites.forEach { fav ->
                        FavoriteBadge(fav = fav, onFocus = {
                            // Point camera straight onto favorite spot
                            cameraOffsetLat = fav.latitude - locationState.latitude
                            cameraOffsetLng = fav.longitude - locationState.longitude
                            isCameraPanned = true
                        }, onDelete = {
                            viewModel.deleteFavoritePlace(fav.id)
                        })
                    }
                }
            }
        }

        // --- ADD FAVORITE PLACE DIALOG ---
        if (showAddFavoriteDialog) {
            AlertDialog(
                onDismissRequest = { showAddFavoriteDialog = false },
                title = { Text("Save Current Pin to Favorites", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Pin coordinates: ${String.format("%.5f", currentLat)}, ${String.format("%.5f", currentLng)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = favPlaceName,
                            onValueChange = { favPlaceName = it },
                            label = { Text("Location Name") },
                            placeholder = { Text("e.g. My Safehouse Home, College Lab") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("fav_name_input")
                        )

                        Text("Location Type", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("HOME", "WORK", "COLLEGE", "OTHER").forEach { type ->
                                val active = favPlaceType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) MaterialTheme.colorScheme.primary else Color.White)
                                        .border(1.dp, if (active) Color.Transparent else Color(0xFFDDE2F0), RoundedCornerShape(8.dp))
                                        .clickable { favPlaceType = type }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else Color(0xFF44474E)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (favPlaceName.isNotBlank()) {
                                viewModel.saveFavoritePlace(favPlaceName.trim(), currentLat, currentLng, favPlaceType)
                                showAddFavoriteDialog = false
                            }
                        },
                        modifier = Modifier.testTag("fav_confirm_btn")
                    ) {
                        Text("Save Place")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddFavoriteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun FloatingMapButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    activeColor: Color = Color.White,
    iconTint: Color = Color(0xFF44474E),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(activeColor)
            .border(1.dp, Color(0xFFE2E7F0), CircleShape)
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun TelemetryInstrumentHUD(
    location: UserLocation,
    onResetOdometer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val lastSyncStr = formatter.format(Date(location.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.78f)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp)
            .border(1.dp, Color(0xFF44474E).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Speed and Compass Dial header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0061A4))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚀", fontSize = 16.sp)
                    }
                    Column {
                        Text(
                            text = "${String.format("%.1f", location.speed)} km/h",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text("Current Speed", color = Color(0xFFA0B0C0), fontSize = 9.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
                        val dirIdx = (((location.bearing + 22.5) % 360) / 45).toInt()
                        val headingStr = directions[dirIdx]
                        Text(
                            text = "$headingStr ${location.bearing.roundToInt()}°",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text("Direction Angle", color = Color(0xFFA0B0C0), fontSize = 9.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9800))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧭", fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF44474E).copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // Sub-metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Altitude
                Column {
                    Text("ALTITUDE", color = Color(0xFFA0B0C0), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text("${location.altitude.roundToInt()} m", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Accuracy
                Column {
                    Text("GPS ACCURACY", color = Color(0xFFA0B0C0), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text("±${String.format("%.1f", location.accuracy)}m", color = Color(0xFF22C55E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Odometer
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("DISTANCE", color = Color(0xFFA0B0C0), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Distance",
                            tint = Color(0xFFD1E1FF),
                            modifier = Modifier
                                .size(12.dp)
                                .clickable { onResetOdometer() }
                        )
                    }
                    Text("${String.format("%.2f", location.distanceTraveled)} km", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sync Token: ${location.userId.take(8)}",
                    fontSize = 8.sp,
                    color = Color(0xFF6F7A8A)
                )
                Text(
                    text = "Cloud Sat Status: SECURE [$lastSyncStr]",
                    fontSize = 8.sp,
                    color = Color(0xFF22C55E),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FavoriteBadge(
    fav: FavoritePlace,
    onFocus: () -> Unit,
    onDelete: () -> Unit
) {
    val emoji = when (fav.type.uppercase()) {
        "HOME" -> "🏠"
        "WORK" -> "💼"
        "COLLEGE" -> "🏫"
        else -> "📍"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFDEE2F9), RoundedCornerShape(12.dp))
            .clickable(onClick = onFocus)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(emoji, fontSize = 14.sp)
            Column {
                Text(
                    text = fav.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${String.format("%.4f", fav.latitude)}, ${String.format("%.4f", fav.longitude)}",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Delete spot", tint = Color.Red, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
fun MapCanvasEngine(
    centerLat: Double,
    centerLng: Double,
    userLat: Double,
    userLng: Double,
    zoom: Float,
    bearing: Float,
    route: List<Pair<Double, Double>>,
    favorites: List<FavoritePlace>,
    viewMode: String,
    trafficEnabled: Boolean,
    onDrag: (Float, Float) -> Unit
) {
    val isSatellite = viewMode == "SATELLITE"
    val isTerrain = viewMode == "TERRAIN"

    // Theme color palettes based on ViewMode
    val mapBgColor = when {
        isSatellite -> Color(0xFF1B2A47) // Photo Navy Blue satellite base
        isTerrain -> Color(0xFFE8E5D9) // Topographical Warm sand/tan
        else -> Color(0xFFF4F3F0) // Clean vector off-white Normal map
    }

    val gridLineColor = when {
        isSatellite -> Color(0xFF2C3E60)
        isTerrain -> Color(0xFFD2CDBC)
        else -> Color(0xFFE5E4E0)
    }

    val parkColor = when {
        isSatellite -> Color(0xFF144D34)
        isTerrain -> Color(0xFFCCE2CB)
        else -> Color(0xFFD2F1D2)
    }

    val waterColor = when {
        isSatellite -> Color(0xFF0F3A66)
        isTerrain -> Color(0xFFABC5DE)
        else -> Color(0xFFAADAFF)
    }

    val roadColor = when {
        isSatellite -> Color(0xFF4B5B6D)
        isTerrain -> Color(0xFFFCF9F2)
        else -> Color.White
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(mapBgColor)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height / 2f

        // Zoom scale translates coordinate differences to canvas pixels
        // Higher Zoom value = Higher multiplier = Closer/Larger elements
        val scaleMultiplier = 3500f * (zoom - 10f)

        // Helper to convert arbitrary relative Lat/Lng to local Canvas Offset
        fun toCanvasOffset(lat: Double, lng: Double): Offset {
            val dx = (lng - centerLng) * scaleMultiplier * cos(Math.toRadians(centerLat))
            val dy = -(lat - centerLat) * scaleMultiplier // Negated because Y is downward in Canvas
            return Offset(cx + dx.toFloat(), cy + dy.toFloat())
        }

        // 1. Draw coordinate grid lines (latitude & longitude indicators)
        val gridSpacing = 80.dp.toPx()
        val columns = (width / gridSpacing).toInt() + 2
        val rows = (height / gridSpacing).toInt() + 2
        for (i in -1..columns) {
            val lineX = (i * gridSpacing)
            drawLine(color = gridLineColor, start = Offset(lineX, 0f), end = Offset(lineX, height), strokeWidth = 1.dp.toPx())
        }
        for (i in -1..rows) {
            val lineY = (i * gridSpacing)
            drawLine(color = gridLineColor, start = Offset(0f, lineY), end = Offset(width, lineY), strokeWidth = 1.dp.toPx())
        }

        // 2. Draw Topography contours if terrain mode
        if (isTerrain) {
            for (radius in listOf(120.dp, 220.dp, 360.dp)) {
                drawCircle(
                    color = Color(0xFFC4BFAA).copy(alpha = 0.5f),
                    radius = radius.toPx(),
                    center = Offset(cx - 50f, cy + 100f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // 3. Draw a mock river / water body
        val riverPath = Path().apply {
            moveTo(0f, height * 0.25f)
            cubicTo(
                width * 0.35f, height * 0.18f,
                width * 0.65f, height * 0.45f,
                width, height * 0.35f
            )
            lineTo(width, height * 0.42f)
            cubicTo(
                width * 0.65f, height * 0.52f,
                width * 0.35f, height * 0.25f,
                0f, height * 0.32f
            )
            close()
        }
        drawPath(path = riverPath, color = waterColor)

        // 4. Draw a large public park / reservation square
        val parkOffset = toCanvasOffset(centerLat + 0.005, centerLng - 0.006)
        val parkW = 180.dp.toPx()
        val parkH = 120.dp.toPx()
        drawRect(
            color = parkColor,
            topLeft = Offset(parkOffset.x - parkW / 2, parkOffset.y - parkH / 2),
            size = androidx.compose.ui.geometry.Size(parkW, parkH)
        )

        // 5. Draw vector roads
        val roads = listOf(
            // Horizontal highway
            Pair(Offset(0f, cy), Offset(width, cy)),
            // Vertical city boulevard
            Pair(Offset(cx, 0f), Offset(cx, height)),
            // Slanted orbital lane
            Pair(Offset(0f, height * 0.1f), Offset(width, height * 0.9f))
        )
        roads.forEach { road ->
            drawLine(color = roadColor, start = road.first, end = road.second, strokeWidth = 12.dp.toPx())
            drawLine(
                color = if (isSatellite) Color(0xFF7B8FA6) else Color(0xFFE2E7F0),
                start = road.first,
                end = road.second,
                strokeWidth = 2.dp.toPx()
            )
        }

        // 6. Draw glowing Traffic Veins if enabled
        if (trafficEnabled) {
            // Draw colorful segments on top of roads
            drawLine(color = Color(0xFF22C55E), start = Offset(0f, cy), end = Offset(cx, cy), strokeWidth = 3.dp.toPx()) // Smooth green
            drawLine(color = Color(0xFFEF4444), start = Offset(cx, cy), end = Offset(width, cy), strokeWidth = 3.dp.toPx()) // Jammed red
            drawLine(color = Color(0xFFF59E0B), start = Offset(cx, 0f), end = Offset(cx, cy), strokeWidth = 3.dp.toPx()) // Slow yellow
            drawLine(color = Color(0xFF22C55E), start = Offset(cx, cy), end = Offset(cx, height), strokeWidth = 3.dp.toPx())
        }

        // 7. Draw historical Polyline Path route points
        if (route.size > 1) {
            val routePath = Path()
            val startOffset = toCanvasOffset(route[0].first, route[0].second)
            routePath.moveTo(startOffset.x, startOffset.y)
            for (idx in 1 until route.size) {
                val ptOffset = toCanvasOffset(route[idx].first, route[idx].second)
                routePath.lineTo(ptOffset.x, ptOffset.y)
            }
            drawPath(
                path = routePath,
                color = Color(0xFF0061A4),
                style = Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }

        // 8. Draw Favorite Places Markers
        favorites.forEach { fav ->
            val favOffset = toCanvasOffset(fav.latitude, fav.longitude)

            // Draw shadow ring
            drawCircle(color = Color.Black.copy(alpha = 0.25f), radius = 10.dp.toPx(), center = favOffset)

            // Pin marker core
            val pinColor = when (fav.type.uppercase()) {
                "HOME" -> Color(0xFF0061A4)
                "WORK" -> Color(0xFFFF9800)
                "COLLEGE" -> Color(0xFF9C27B0)
                else -> Color(0xFF757575)
            }
            drawCircle(color = pinColor, radius = 7.dp.toPx(), center = favOffset)
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = favOffset)
        }

        // 9. Draw the User Active Locator Marker (Pulse + Marker Core + Heading vector)
        val userOffset = toCanvasOffset(userLat, userLng)

        // Radar Pulse aura
        val animationScale = (System.currentTimeMillis() % 2000) / 2000f
        drawCircle(
            color = Color(0xFF0061A4).copy(alpha = 0.35f * (1f - animationScale)),
            radius = 24.dp.toPx() * animationScale,
            center = userOffset
        )

        // Arrow head pointing to bearing direction
        val bearingRad = Math.toRadians(bearing.toDouble())
        val arrowLength = 16.dp.toPx()
        val arrowEndX = userOffset.x + arrowLength * sin(bearingRad).toFloat()
        val arrowEndY = userOffset.y - arrowLength * cos(bearingRad).toFloat() // Negative because Y is downward

        drawLine(
            color = Color(0xFF0061A4),
            start = userOffset,
            end = Offset(arrowEndX, arrowEndY),
            strokeWidth = 3.dp.toPx()
        )

        // Circle core locator
        drawCircle(color = Color.White, radius = 9.dp.toPx(), center = userOffset)
        drawCircle(color = Color(0xFF0061A4), radius = 6.dp.toPx(), center = userOffset)
    }
}
