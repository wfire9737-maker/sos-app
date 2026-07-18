package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EmergencyHistoryItem
import com.example.ui.GuardianViewModel
import kotlinx.coroutines.delay

import com.example.ui.theme.SkeletonCard
import com.example.ui.theme.SkeletonLine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyHistoryScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val history by viewModel.emergencyHistory.collectAsState()

    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(450)
        isLoading = false
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedSeverityFilter by remember { mutableStateOf("ALL") } // "ALL", "CRITICAL", "HIGH", "WARNING"
    var selectedSortBy by remember { mutableStateOf("DATE_DESC") } // "DATE_DESC", "DATE_ASC", "DURATION_DESC", "RESPONSE_ASC"

    var selectedItemForDetail by remember { mutableStateOf<EmergencyHistoryItem?>(null) }
    var selectedItemForDelete by remember { mutableStateOf<EmergencyHistoryItem?>(null) }

    var showExportConfirmDialog by remember { mutableStateOf(false) }
    var exportType by remember { mutableStateOf("") } // "CSV" or "PDF"
    var exportContentResult by remember { mutableStateOf("") }

    // Statistics Calculation
    val totalEvents = history.size
    val avgResponseTime = if (history.isNotEmpty()) history.map { it.responseTimeSeconds }.average().toInt() else 0
    val avgDuration = if (history.isNotEmpty()) history.map { it.durationSeconds }.average().toInt() else 0
    val avgAiScore = if (history.isNotEmpty()) history.map { it.aiScore }.average().toInt() else 0

    // Filtered & Sorted List
    val processedHistory = remember(history, searchQuery, selectedSeverityFilter, selectedSortBy) {
        history.filter { item ->
            val matchesSearch = item.locationName.contains(searchQuery, ignoreCase = true) ||
                    item.deviceUsed.contains(searchQuery, ignoreCase = true) ||
                    item.triggerType.contains(searchQuery, ignoreCase = true) ||
                    item.resolutionNotes.contains(searchQuery, ignoreCase = true)

            val matchesSeverity = when (selectedSeverityFilter) {
                "CRITICAL" -> item.severity.equals("CRITICAL", ignoreCase = true)
                "HIGH" -> item.severity.equals("HIGH", ignoreCase = true)
                "WARNING" -> item.severity.equals("WARNING", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesSeverity
        }.sortedWith { a, b ->
            when (selectedSortBy) {
                "DATE_ASC" -> a.date.compareTo(b.date)
                "DURATION_DESC" -> b.durationSeconds.compareTo(a.durationSeconds)
                "RESPONSE_ASC" -> a.responseTimeSeconds.compareTo(b.responseTimeSeconds)
                else -> b.date.compareTo(a.date) // "DATE_DESC" default
            }
        }
    }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "Emergency History Logs",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("history_title")
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Export CSV
                    IconButton(
                        onClick = {
                            exportType = "CSV"
                            exportContentResult = viewModel.getHistoryCSVString()
                            showExportConfirmDialog = true
                        },
                        modifier = Modifier.testTag("export_csv_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = "Export CSV",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Export PDF Report
                    IconButton(
                        onClick = {
                            exportType = "PDF"
                            exportContentResult = viewModel.getHistoryPDFReportText()
                            showExportConfirmDialog = true
                        },
                        modifier = Modifier.testTag("export_pdf_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF",
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
            // --- STATISTICS PANEL ---
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_stats_panel")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatMetricColumn(
                        value = totalEvents.toString(),
                        label = "Total SOS",
                        icon = Icons.Default.Emergency,
                        color = Color(0xFFD50000),
                        modifier = Modifier.weight(1f)
                    )
                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    )
                    StatMetricColumn(
                        value = "${avgResponseTime}s",
                        label = "Avg Resp",
                        icon = Icons.Default.Bolt,
                        color = Color(0xFFFF8F00),
                        modifier = Modifier.weight(1f)
                    )
                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    )
                    StatMetricColumn(
                        value = "${avgDuration}s",
                        label = "Avg Active",
                        icon = Icons.Default.HourglassEmpty,
                        color = Color(0xFF1E88E5),
                        modifier = Modifier.weight(1f)
                    )
                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    )
                    StatMetricColumn(
                        value = "$avgAiScore%",
                        label = "AI Acc.",
                        icon = Icons.Default.AutoAwesome,
                        color = Color(0xFF43A047),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- SEARCH AND FILTERS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search location, device, notes...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("history_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Sorting drop-down icon/trigger
                Box {
                    var showSortMenu by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("sort_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort history logs"
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Newest Date First") },
                            onClick = {
                                selectedSortBy = "DATE_DESC"
                                showSortMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.ArrowDownward, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest Date First") },
                            onClick = {
                                selectedSortBy = "DATE_ASC"
                                showSortMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.ArrowUpward, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Longest Active Duration") },
                            onClick = {
                                selectedSortBy = "DURATION_DESC"
                                showSortMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.HourglassFull, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Fastest Response Time") },
                            onClick = {
                                selectedSortBy = "RESPONSE_ASC"
                                showSortMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Speed, null) }
                        )
                    }
                }
            }

            // --- FILTER CHIPS Row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedSeverityFilter == "ALL",
                    onClick = { selectedSeverityFilter = "ALL" },
                    label = { Text("All Logs") },
                    modifier = Modifier.testTag("severity_all_chip")
                )
                FilterChip(
                    selected = selectedSeverityFilter == "CRITICAL",
                    onClick = { selectedSeverityFilter = "CRITICAL" },
                    label = { Text("Critical") },
                    modifier = Modifier.testTag("severity_critical_chip")
                )
                FilterChip(
                    selected = selectedSeverityFilter == "HIGH",
                    onClick = { selectedSeverityFilter = "HIGH" },
                    label = { Text("High") },
                    modifier = Modifier.testTag("severity_high_chip")
                )
                FilterChip(
                    selected = selectedSeverityFilter == "WARNING",
                    onClick = { selectedSeverityFilter = "WARNING" },
                    label = { Text("Warning") },
                    modifier = Modifier.testTag("severity_warning_chip")
                )
            }

            // --- HISTORY LIST ---
            if (isLoading) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(4) {
                        SkeletonCard(height = 130.dp, borderRadius = 16.dp)
                    }
                }
            } else if (processedHistory.isEmpty()) {
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
                        Text("📜", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No history records found",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Resolve active SOS distress events to record fresh telemetry.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("history_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = processedHistory,
                        key = { it.id }
                    ) { item ->
                        HistoryCard(
                            item = item,
                            onClick = { selectedItemForDetail = item },
                            onDelete = { selectedItemForDelete = item }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG: EVENT DETAILS ---
    selectedItemForDetail?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedItemForDetail = null },
            icon = {
                Icon(
                    imageVector = when (item.severity.uppercase()) {
                        "CRITICAL" -> Icons.Default.Emergency
                        "HIGH" -> Icons.Default.Warning
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = when (item.severity.uppercase()) {
                        "CRITICAL" -> Color(0xFFD50000)
                        "HIGH" -> Color(0xFFFF8F00)
                        else -> Color(0xFF1E88E5)
                    },
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "${item.triggerType.replace("_", " ")} Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailTextRow("Date / Time", "${item.date} at ${item.time}")
                    DetailTextRow("Severity Grade", item.severity)
                    DetailTextRow("Active Duration", "${item.durationSeconds} seconds")
                    DetailTextRow("Response Delay", "${item.responseTimeSeconds} seconds")
                    DetailTextRow("Location Plot", item.locationName)
                    DetailTextRow("Coordinates", "Lat: ${item.latitude}, Lng: ${item.longitude}")
                    DetailTextRow("Device Node", item.deviceUsed)
                    DetailTextRow("AI Fall Confidence", "${item.aiScore}%")
                    DetailTextRow("Contacts Paged", item.contactsNotified.joinToString(", "))
                    DetailTextRow("Resolved By", item.resolvedBy)
                    DetailTextRow("Triage Notes", item.resolutionNotes)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { selectedItemForDetail = null },
                    modifier = Modifier.testTag("close_detail_dialog")
                ) {
                    Text("Close")
                }
            }
        )
    }

    // --- DIALOG: CONFIRM DELETE ---
    selectedItemForDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedItemForDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Event Record?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete the SOS event on ${item.date} at ${item.time}? This action is irreversible."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHistoryItem(item.id)
                        selectedItemForDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_history_btn")
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedItemForDelete = null },
                    modifier = Modifier.testTag("cancel_delete_history_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- DIALOG: EXPORT COMPLETE ---
    if (showExportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExportConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = if (exportType == "CSV") Icons.Default.InsertDriveFile else Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Export ${exportType} Report",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "The telemetry report in ${exportType} format was formatted successfully. Click 'Share & Send' below to dispatch the document logs."
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = exportContentResult,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Launch android generic share intent
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Guardian SOS - Emergency Telemetry $exportType")
                            putExtra(Intent.EXTRA_TEXT, exportContentResult)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Telemetry Report via"))
                        showExportConfirmDialog = false
                    },
                    modifier = Modifier.testTag("share_export_btn")
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share & Send")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExportConfirmDialog = false },
                    modifier = Modifier.testTag("close_export_btn")
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun DetailTextRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
fun StatMetricColumn(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun HistoryCard(
    item: EmergencyHistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val severityColor = when (item.severity.uppercase()) {
        "CRITICAL" -> Color(0xFFD50000)
        "HIGH" -> Color(0xFFFF8F00)
        else -> Color(0xFF1E88E5)
    }

    val icon = when (item.triggerType) {
        "FALL_DETECTED" -> Icons.Default.DirectionsRun
        else -> Icons.Default.Emergency
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left icon representing trigger category
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(severityColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = severityColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Middle telemetry summary
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.triggerType.replace("_", " "),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = item.date,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.locationName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tags for AI Score and Response time
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(severityColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.severity,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = severityColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AI: ${item.aiScore}%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Resp: ${item.responseTimeSeconds}s",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Actions - click to delete record
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_history_btn_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Event Log",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
