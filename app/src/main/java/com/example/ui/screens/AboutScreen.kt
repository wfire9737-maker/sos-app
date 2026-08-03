package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.clickable
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch
import com.example.ui.GuardianViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun AboutScreen(viewModel: GuardianViewModel, onNavigateBack: () -> Unit) {
    val developerModeEnabled by viewModel.developerModeEnabled.collectAsState()
    var clickCount by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("About Smart SOS", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Smart SOS", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                "Version 1.0.0", 
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.clickable {
                    if (!developerModeEnabled) {
                        clickCount++
                        if (clickCount >= 7) {
                            viewModel.setDeveloperModeEnabled(true)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Developer Mode Activated. Warning: Intended for testing.")
                            }
                            clickCount = 0
                        } else if (clickCount >= 3) {
                            val remaining = 7 - clickCount
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("You are $remaining steps away from being a developer.")
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Developer Mode is already enabled.")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your personal safety companion.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
