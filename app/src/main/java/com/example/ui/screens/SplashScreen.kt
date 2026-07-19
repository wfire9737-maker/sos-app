package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.ui.GuardianViewModel

@Composable
fun SplashScreen(viewModel: GuardianViewModel, onNavigateToNext: () -> Unit) {
    LaunchedEffect(key1 = Unit) {
        delay(1500)
        onNavigateToNext()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text("Smart SOS", color = MaterialTheme.colorScheme.onPrimary, fontSize = 48.sp, fontWeight = FontWeight.Bold)
    }
}
