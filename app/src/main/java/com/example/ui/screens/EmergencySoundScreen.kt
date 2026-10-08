package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.GuardianViewModel
import com.example.ui.components.SettingsSection
import com.example.ui.components.SettingsSwitchItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencySoundScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE) }

    val emergencySoundId by viewModel.emergencySoundId.collectAsState()
    val emergencyCustomSoundName by viewModel.emergencyCustomSoundName.collectAsState()
    val sosSoundEnabled by viewModel.sosSoundEnabled.collectAsState()

    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    val stopPreview = {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
        } catch (_: Exception) {}
        previewPlayer = null
        isPreviewPlaying = false
    }

    val startPreview: (String) -> Unit = { soundId ->
        stopPreview()
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val player = when (soundId) {
                "builtin_rapid_alarm" -> MediaPlayer.create(context, R.raw.emergency_rapid_alarm)
                "builtin_warning_pulse" -> MediaPlayer.create(context, R.raw.emergency_warning_pulse)
                "builtin_double_beep" -> MediaPlayer.create(context, R.raw.emergency_double_beep)
                "builtin_critical_alert" -> MediaPlayer.create(context, R.raw.emergency_critical_alert)
                "builtin_evacuation_tone" -> MediaPlayer.create(context, R.raw.emergency_evacuation_tone)
                "custom" -> {
                    val customUriStr = prefs.getString("emergency_custom_sound_uri", null)
                    if (!customUriStr.isNullOrBlank()) {
                        try {
                            MediaPlayer().apply {
                                setAudioAttributes(audioAttributes)
                                setDataSource(context, Uri.parse(customUriStr))
                                prepare()
                            }
                        } catch (e: Exception) {
                            Log.w("EmergencySoundScreen", "Failed to preview custom sound, falling back to siren: ${e.message}")
                            MediaPlayer.create(context, R.raw.sos_emergency_siren)
                        }
                    } else {
                        MediaPlayer.create(context, R.raw.sos_emergency_siren)
                    }
                }
                else -> MediaPlayer.create(context, R.raw.sos_emergency_siren)
            }

            player?.let { p ->
                p.setOnCompletionListener {
                    stopPreview()
                }
                p.start()
                previewPlayer = p
                isPreviewPlaying = true

                coroutineScope.launch {
                    delay(4000)
                    if (previewPlayer == p) {
                        stopPreview()
                    }
                }
            } ?: run {
                Toast.makeText(context, "Failed to load audio preview", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("EmergencySoundScreen", "Preview playback failed: ${e.message}")
            stopPreview()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPreview()
        }
    }

    val customSoundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                var canOpen = false
                try {
                    context.contentResolver.openInputStream(uri)?.use {
                        canOpen = true
                    }
                } catch (_: Exception) {}

                if (canOpen) {
                    var displayName: String? = null
                    try {
                        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1) {
                                    displayName = cursor.getString(nameIndex)
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    val resolvedName = displayName ?: uri.lastPathSegment ?: "Custom Sound"
                    viewModel.setEmergencyCustomSound(uri.toString(), resolvedName)
                    Toast.makeText(context, "Selected: $resolvedName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Cannot read selected audio file. Keeping previous sound.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error importing audio: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val soundOptions = listOf(
        "builtin_siren" to "Built-in Siren",
        "builtin_rapid_alarm" to "Rapid Alarm",
        "builtin_warning_pulse" to "Warning Pulse",
        "builtin_double_beep" to "Double Beep",
        "builtin_critical_alert" to "Critical Alert",
        "builtin_evacuation_tone" to "Evacuation Tone",
        "custom" to "Custom Sound"
    )

    val selectedSoundName = when (emergencySoundId) {
        "builtin_rapid_alarm" -> "Rapid Alarm"
        "builtin_warning_pulse" -> "Warning Pulse"
        "builtin_double_beep" -> "Double Beep"
        "builtin_critical_alert" -> "Critical Alert"
        "builtin_evacuation_tone" -> "Evacuation Tone"
        "custom" -> if (!emergencyCustomSoundName.isNullOrBlank()) "Custom: $emergencyCustomSoundName" else "Custom Sound"
        else -> "Built-in Siren"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Emergency Sound",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("emergency_sound_title")
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("emergency_sound_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            // SOS Trigger Sound Switch
            item {
                SettingsSection(title = "Trigger Behavior") {
                    SettingsSwitchItem(
                        icon = if (sosSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        title = "SOS Trigger Sound",
                        subtitle = if (sosSoundEnabled) "Sound siren automatically when SOS triggers" else "Silent SOS mode (alarm sound disabled)",
                        checked = sosSoundEnabled,
                        onCheckedChange = { enabled -> viewModel.setSosSoundEnabled(enabled) }
                    )
                }
            }

            // Current Sound & Audio Preview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Current Alert Sound",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = selectedSoundName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (isPreviewPlaying) {
                                    stopPreview()
                                } else {
                                    startPreview(emergencySoundId)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = if (isPreviewPlaying) {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            } else {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            },
                            modifier = Modifier.testTag("preview_sound_button")
                        ) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPreviewPlaying) "Stop Preview" else "Preview Sound",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPreviewPlaying) "Stop" else "Preview")
                        }
                    }
                }
            }

            // Sound Options List
            item {
                SettingsSection(title = "Alert Tone Selection") {
                    Text(
                        text = "Select the tone that plays on your phone when an emergency starts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    soundOptions.forEach { (id, title) ->
                        val isSelected = emergencySoundId == id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable {
                                    if (id == "custom" && prefs.getString("emergency_custom_sound_uri", null).isNullOrBlank()) {
                                        customSoundPickerLauncher.launch(arrayOf("audio/*"))
                                    } else {
                                        viewModel.setEmergencySoundId(id)
                                    }
                                }
                                .testTag("sound_option_$id"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                            tonalElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        if (id == "custom" && prefs.getString("emergency_custom_sound_uri", null).isNullOrBlank()) {
                                            customSoundPickerLauncher.launch(arrayOf("audio/*"))
                                        } else {
                                            viewModel.setEmergencySoundId(id)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (id == "custom") {
                                        val customName = emergencyCustomSoundName
                                        Text(
                                            text = if (!customName.isNullOrBlank()) customName else "No custom sound selected.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (!customName.isNullOrBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (id == "custom") {
                                    OutlinedButton(
                                        onClick = { customSoundPickerLauncher.launch(arrayOf("audio/*")) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("custom_sound_picker_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.UploadFile,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (!emergencyCustomSoundName.isNullOrBlank()) "Change" else "Import",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
