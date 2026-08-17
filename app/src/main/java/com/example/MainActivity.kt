package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
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
    enableEdgeToEdge()
    setContent {
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
    

    val resolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { _ -> }

    val promptEnableLocation = {
        var currentContext = context
        var activity: android.app.Activity? = null
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is android.app.Activity) {
                activity = currentContext
                break
            }
            currentContext = currentContext.baseContext
        }
        
        if (activity != null) {
            try {
                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 10000
                ).build()
                
                val builder = com.google.android.gms.location.LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest)
                    .setAlwaysShow(true)
                    
                val client = com.google.android.gms.location.LocationServices.getSettingsClient(activity)
                val task = client.checkLocationSettings(builder.build())
                
                task.addOnFailureListener { exception ->
                    if (exception is com.google.android.gms.common.api.ResolvableApiException) {
                        try {
                            val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(exception.resolution).build()
                            resolutionLauncher.launch(intentSenderRequest)
                        } catch (sendEx: Throwable) {
                            try {
                                activity.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            } catch (e: Throwable) {}
                        }
                    } else {
                        // Play services not available or other error, fallback to settings
                        try {
                            activity.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } catch (e: Throwable) {}
                    }
                }
            } catch (e: Throwable) {
                try {
                    activity.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } catch (ex: Throwable) {}
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            promptEnableLocation()
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
                    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                    if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                        promptEnableLocation()
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
