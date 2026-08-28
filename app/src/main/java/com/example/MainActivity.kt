package com.example

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.GuardianViewModel
import com.example.ui.navigation.NavGraph
import com.example.ui.theme.GuardianTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        val stackTrace = android.util.Log.getStackTraceString(throwable)
        getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("last_crash", stackTrace)
            .commit()
        defaultHandler?.uncaughtException(thread, throwable)
    }
    
    val lastCrash = getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)
        .getString("last_crash", null)
    if (lastCrash != null) {
        android.util.Log.e("CRASH_LOG", "Last crash: $lastCrash")
    }

    try {
        enableEdgeToEdge()
        setContent {
          if (lastCrash != null) {
             Box(
                 modifier = Modifier.fillMaxSize().background(Color.Red)
             ) {
                 LazyColumn {
                     item {
                         Text(
                             text = lastCrash,
                             color = Color.White,
                             modifier = Modifier.padding(16.dp)
                         )
                     }
                     item {
                         androidx.compose.material3.Button(onClick = {
                             getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)
                                .edit()
                                .remove("last_crash")
                                .commit()
                         }, modifier = Modifier.padding(16.dp)) {
                             Text("Clear Crash Log")
                         }
                     }
                 }
             }
             return@setContent
          }
          val guardianViewModel: GuardianViewModel = androidx.hilt.navigation.compose.hiltViewModel()
          val themeMode by guardianViewModel.themeMode.collectAsState()
          val isDarkTheme = when (themeMode) {
            "DARK" -> true
            "LIGHT" -> false
            else -> isSystemInDarkTheme()
          }
          GuardianTheme(darkTheme = isDarkTheme) {
            Surface(
              modifier = Modifier.fillMaxSize(),
              color = androidx.compose.material3.MaterialTheme.colorScheme.background
            ) {
              AppPermissionChecker()
              NavGraph(viewModel = guardianViewModel)
            }
          }
        }
    } catch (e: Throwable) {
        val stackTrace = android.util.Log.getStackTraceString(e)
        System.err.println("CRASH_IN_MAIN_ACTIVITY: $stackTrace")
        throw e
    }
  }
}

@Composable
fun AppPermissionChecker() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val permissionsToRequest = mutableListOf(
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.CALL_PHONE,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.READ_CONTACTS
    )
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
    }
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_SCAN)
        permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_CONNECT)
    }

    fun promptEnableLocation() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Please enable Location Services", Toast.LENGTH_LONG).show()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted || (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && 
             androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)) {
            try {
                com.example.service.BleForegroundService.start(context)
            } catch (e: Exception) {
                // Ignore
            }
        }
        
            
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        try {
            if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                promptEnableLocation()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                val missingPermissions = permissionsToRequest.toList().filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }
                if (missingPermissions.isNotEmpty()) {
                    launcher.launch(missingPermissions.toTypedArray())
                } else {
                    try { com.example.service.BleForegroundService.start(context) } catch (e: Exception) {}
                    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                    try {
                        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                            promptEnableLocation()
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
