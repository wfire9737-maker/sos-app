package com.example

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.service.BleForegroundService
import com.example.service.NearbyBleService
import com.example.ui.GuardianViewModel
import com.example.ui.navigation.NavGraph
import com.example.ui.theme.GuardianTheme
import com.example.utils.hasPermission
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d("CLOUD_DEBUG", "BUILD_MARKER version=1.0.0")
    try {
        enableEdgeToEdge()
        setContent {
          val guardianViewModel: GuardianViewModel = androidx.hilt.navigation.compose.hiltViewModel()
          val themeMode by guardianViewModel.themeMode.collectAsState()
          val highContrast by guardianViewModel.highContrast.collectAsState()
          val isDarkTheme = when (themeMode) {
            "DARK" -> true
            "LIGHT" -> false
            else -> isSystemInDarkTheme()
          }
          GuardianTheme(darkTheme = isDarkTheme, highContrast = highContrast) {
            Surface(
              modifier = Modifier.fillMaxSize(),
              color = MaterialTheme.colorScheme.background
            ) {
              AppPermissionChecker(guardianViewModel)
              NavGraph(viewModel = guardianViewModel)
            }
          }
        }
    } catch (e: Throwable) {
        val stackTrace = Log.getStackTraceString(e)
        System.err.println("CRASH_IN_MAIN_ACTIVITY: $stackTrace")
        throw e
    }
  }
}

@Composable
fun AppPermissionChecker(viewModel: GuardianViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasRequestedBluetooth by rememberSaveable { mutableStateOf(false) }
    var hasRequestedLocation by rememberSaveable { mutableStateOf(false) }

    val permissionsToRequest = mutableListOf(
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.CALL_PHONE,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.READ_CONTACTS
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_SCAN)
        permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_CONNECT)
    }

    val locationResolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { _ ->
        // System location dialog closed (user accepted or declined).
        // hasRequestedLocation remains true to prevent repeated prompts.
    }

    fun checkAndRequestLocation() {
        val hasLocationPermission = context.hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ||
                context.hasPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)

        // State A: Location permission denied -> do not trigger device location enable prompt
        if (!hasLocationPermission) return

        // Prevent repeated prompts during current session / lifecycle loops
        if (hasRequestedLocation) return

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isLocationOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager?.isLocationEnabled == true
        } else {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        }

        // State C: Location permission granted and device Location is ON -> do nothing
        if (isLocationOn) return

        // State B: Location permission granted but device Location is OFF -> show prompt via ResolvableApiException
        hasRequestedLocation = true
        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 10000L
            ).build()

            val settingsRequest = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true)
                .build()

            val client = LocationServices.getSettingsClient(context)
            client.checkLocationSettings(settingsRequest)
                .addOnFailureListener { exception ->
                    if (exception is ResolvableApiException) {
                        try {
                            val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                            locationResolutionLauncher.launch(intentSenderRequest)
                        } catch (sendEx: Exception) {
                            Log.e("MainActivity", "Failed to launch location resolution intent", sendEx)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking location settings", e)
        }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val isEnabled = btManager?.adapter?.isEnabled == true || result.resultCode == Activity.RESULT_OK
        if (isEnabled) {
            try {
                viewModel.deviceService.bleManager.scanAndConnect()
            } catch (e: Exception) {
                // Ignore
            }
        }
        checkAndRequestLocation()
    }

    fun checkAndRequestBluetooth(): Boolean {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter ?: return false
        if (!adapter.isEnabled && !hasRequestedBluetooth) {
            val canRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                true
            }
            if (canRequest) {
                hasRequestedBluetooth = true
                try {
                    enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    return true
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to launch Bluetooth enable request", e)
                }
            }
        }
        return false
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val hasBlePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.hasPermission(android.Manifest.permission.BLUETOOTH_SCAN) &&
                    context.hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            true
        }
        if (hasBlePermissions) {
            try {
                BleForegroundService.start(context)
            } catch (e: Exception) {
                // Ignore
            }
        }
        try {
            NearbyBleService.startOrStop(context)
        } catch (e: Exception) {
            // Ignore
        }
        val bluetoothPromptShown = checkAndRequestBluetooth()
        if (!bluetoothPromptShown) {
            checkAndRequestLocation()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                val missingPermissions = permissionsToRequest.toList().filter {
                    !context.hasPermission(it)
                }
                val hasBlePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.hasPermission(android.Manifest.permission.BLUETOOTH_SCAN) &&
                            context.hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    true
                }

                if (hasBlePermissions) {
                    try {
                        BleForegroundService.start(context)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
                try {
                    NearbyBleService.startOrStop(context)
                } catch (e: Exception) {
                    // Ignore
                }

                if (missingPermissions.isNotEmpty()) {
                    permissionLauncher.launch(missingPermissions.toTypedArray())
                } else {
                    val bluetoothPromptShown = checkAndRequestBluetooth()
                    if (!bluetoothPromptShown) {
                        checkAndRequestLocation()
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
