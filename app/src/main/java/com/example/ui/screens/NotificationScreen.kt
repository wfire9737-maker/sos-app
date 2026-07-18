package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NotificationModel
import com.example.model.NotificationCategory
import com.example.ui.GuardianViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val notifications by viewModel.notificationsNew.collectAsState()
    val fcmToken by viewModel.fcmToken.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "UNREAD", "EMERGENCY", "SYSTEM"

    // Filtered notifications list
    val filteredNotifications = remember(notifications, searchQuery, selectedFilter) {
        notifications.filter { item ->
            val matchesSearch = item.title.contains(searchQuery, ignoreCase = true) ||
                    item.body.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "UNREAD" -> !item.isRead
                "EMERGENCY" -> item.category == NotificationCategory.EMERGENCY_ALERTS || item.category == NotificationCategory.EMERGENCY_RESOLVED
                "SYSTEM" -> item.category != NotificationCategory.EMERGENCY_ALERTS && item.category != NotificationCategory.EMERGENCY_RESOLVED
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Alert Notifications",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("notifications_title")
                        )
                        if (fcmToken.isNotEmpty()) {
                            Text(
                                text = "Token: ${fcmToken.take(12)}...${fcmToken.takeLast(6)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("notifications_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Mark All Read Action
                    IconButton(
                        onClick = { viewModel.markAllNotificationsNewAsRead() },
                        modifier = Modifier.testTag("mark_all_read_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Mark all as read",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- SEARCH BAR ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search notification logs...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notification_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // --- FILTER CHIPS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All") },
                    modifier = Modifier.testTag("filter_all_chip")
                )
                FilterChip(
                    selected = selectedFilter == "UNREAD",
                    onClick = { selectedFilter = "UNREAD" },
                    label = { Text("Unread") },
                    modifier = Modifier.testTag("filter_unread_chip")
                )
                FilterChip(
                    selected = selectedFilter == "EMERGENCY",
                    onClick = { selectedFilter = "EMERGENCY" },
                    label = { Text("Critical") },
                    modifier = Modifier.testTag("filter_emergency_chip")
                )
                FilterChip(
                    selected = selectedFilter == "SYSTEM",
                    onClick = { selectedFilter = "SYSTEM" },
                    label = { Text("System") },
                    modifier = Modifier.testTag("filter_system_chip")
                )
            }

            // --- INTERACTIVE BROADCAST FCM SIMULATOR PANEL ---
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fcm_simulator_panel")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚡ LIVE FCM BROADCAST SIMULATOR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 1st row of simulate buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SimulatorButton(
                            label = "Emergency",
                            color = Color(0xFFD50000),
                            onClick = { viewModel.simulateIncomingNotificationNew(NotificationCategory.EMERGENCY_ALERTS) },
                            modifier = Modifier.weight(1f).testTag("sim_emergency_btn")
                        )
                        SimulatorButton(
                            label = "Low Battery",
                            color = Color(0xFFFF8F00),
                            onClick = { viewModel.simulateIncomingNotificationNew(NotificationCategory.BATTERY_ALERTS) },
                            modifier = Modifier.weight(1f).testTag("sim_battery_btn")
                        )
                        SimulatorButton(
                            label = "Offline",
                            color = Color(0xFF757575),
                            onClick = { viewModel.simulateIncomingNotificationNew(NotificationCategory.DEVICE_DISCONNECTED) },
                            modifier = Modifier.weight(1f).testTag("sim_offline_btn")
                        )
                    }

                    // 2nd row of simulate buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SimulatorButton(
                            label = "GPS Lost",
                            color = Color(0xFF1E88E5),
                            onClick = { viewModel.simulateIncomingNotificationNew(NotificationCategory.GPS_UNAVAILABLE) },
                            modifier = Modifier.weight(1f).testTag("sim_gps_btn")
                        )
                        SimulatorButton(
                            label = "Firmware",
                            color = Color(0xFF8E24AA),
                            onClick = { viewModel.simulateIncomingNotificationNew(NotificationCategory.FIRMWARE_UPDATES) },
                            modifier = Modifier.weight(1f).testTag("sim_firmware_btn")
                        )
                        SimulatorButton(
                            label = "Safe Arrival",
                            color = Color(0xFF43A047),
                            onClick = { viewModel.simulateIncomingNotificationNew(NotificationCategory.SAFE_ARRIVAL) },
                            modifier = Modifier.weight(1f).testTag("sim_arrival_btn")
                        )
                    }

                    // 3rd row: Emergency Cancelled
                    SimulatorButton(
                        label = "Simulate Emergency Cancelled Alarm Reset",
                        color = Color(0xFF00B0FF),
                        onClick = { viewModel.simulateIncomingNotificationNew(NotificationCategory.EMERGENCY_RESOLVED) },
                        modifier = Modifier.fillMaxWidth().height(32.dp).testTag("sim_cancel_btn")
                    )
                }
            }

            // --- NOTIFICATIONS LIST ---
            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("📭", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No notifications found",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Try triggering broadcasts with the simulator deck above!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = filteredNotifications,
                        key = { it.id }
                    ) { item ->
                        NotificationCard(
                            item = item,
                            onMarkRead = { viewModel.markNotificationNewAsRead(item.id) },
                            onDelete = { viewModel.deleteNotificationNew(item.id) },
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimulatorButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        modifier = modifier.height(34.dp)
    ) {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun NotificationCard(
    item: NotificationModel,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeColor = when (item.category) {
        NotificationCategory.EMERGENCY_ALERTS -> Color(0xFFD50000)
        NotificationCategory.EMERGENCY_RESOLVED -> Color(0xFF00B0FF)
        NotificationCategory.BATTERY_ALERTS -> Color(0xFFFF8F00)
        NotificationCategory.DEVICE_DISCONNECTED -> Color(0xFF757575)
        NotificationCategory.GPS_UNAVAILABLE -> Color(0xFF1E88E5)
        NotificationCategory.FIRMWARE_UPDATES -> Color(0xFF8E24AA)
        NotificationCategory.SAFE_ARRIVAL -> Color(0xFF43A047)
    }

    val icon = when (item.category) {
        NotificationCategory.EMERGENCY_ALERTS -> Icons.Default.Emergency
        NotificationCategory.EMERGENCY_RESOLVED -> Icons.Default.CheckCircle
        NotificationCategory.BATTERY_ALERTS -> Icons.Default.BatteryAlert
        NotificationCategory.DEVICE_DISCONNECTED -> Icons.Default.SignalCellularNull
        NotificationCategory.GPS_UNAVAILABLE -> Icons.Default.GpsOff
        NotificationCategory.FIRMWARE_UPDATES -> Icons.Default.SystemUpdate
        NotificationCategory.SAFE_ARRIVAL -> Icons.Default.Home
    }

    val formattedTime = remember(item.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRead) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { if (!item.isRead) onMarkRead() }
            .testTag("notification_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Leading Badge Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Body Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.Bold,
                        color = if (item.isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).testTag("notification_card_title")
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.body,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                if (item.deviceId != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Wearable Ref: ${item.deviceId}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Actions (Dismiss / Delete / Mark Read)
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!item.isRead) {
                    IconButton(
                        onClick = onMarkRead,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("mark_read_btn_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Mark as read",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("delete_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete notification",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
