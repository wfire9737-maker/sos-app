package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GuardianViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpFaqScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val faqs = listOf(
        Pair("How does Fall Detection work?", "The app uses your device's accelerometer and gyroscope to detect sudden impacts and changes in orientation. If a fall is detected, a 15-second countdown begins before alerting your emergency contacts."),
        Pair("What is the Safety Timer?", "The Safety Timer allows you to set a specific duration for an activity. If you don't check in before the timer expires, an automatic SOS is triggered."),
        Pair("How do I add an emergency contact?", "Go to the 'Contacts' tab and tap the '+' button. You can add contacts manually or scan their Guardian QR code."),
        Pair("Is my location data secure?", "Yes. Your location data is encrypted and only shared with your designated emergency contacts during an active SOS or if background location sharing is explicitly enabled."),
        Pair("How do I trigger an SOS?", "You can trigger an SOS by pressing the large red button on the Home screen, using a configured voice wake word, or automatically via fall detection/safety timer.")
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Help & FAQ") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(faqs) { faq ->
                var expanded by remember { mutableStateOf(false) }
                Card(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = faq.first,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        if (expanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = faq.second,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
