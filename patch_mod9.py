import re

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "r") as f:
    content = f.read()

imports = """
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.content.IntentFilter
import kotlinx.coroutines.delay
"""

if "import android.net.ConnectivityManager" not in content:
    content = content.replace("import android.content.ClipData", imports + "import android.content.ClipData")

mod9_code = """
            Spacer(modifier = Modifier.height(24.dp))
            Text("Module 9 - Performance Monitor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            var memoryUsage by remember { mutableStateOf(0L) }
            var networkType by remember { mutableStateOf("Unknown") }
            var currentBattery by remember { mutableStateOf(-1) }
            var gpsAccuracy by remember { mutableStateOf("Unknown") }

            val appVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Exception) {
                "Unknown"
            }
            val androidVersion = android.os.Build.VERSION.RELEASE
            val deviceModel = android.os.Build.MODEL
            
            LaunchedEffect(Unit) {
                while(true) {
                    val runtime = Runtime.getRuntime()
                    memoryUsage = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                    
                    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val network = connectivityManager.activeNetwork
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    networkType = when {
                        capabilities == null -> "None"
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                        else -> "Other"
                    }
                    
                    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                        context.registerReceiver(null, ifilter)
                    }
                    currentBattery = batteryStatus?.let { intent ->
                        val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        if (scale > 0) (level * 100 / scale) else -1
                    } ?: -1
                    
                    gpsAccuracy = if (isLocationPermissionGranted && isGpsEnabled) "High" else "Low"
                    
                    delay(2000)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusItemCustomColor("App Version", appVersion ?: "Unknown", MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusItemCustomColor("Android Version", androidVersion, MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusItemCustomColor("Device Model", deviceModel, MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusItemCustomColor("Memory Usage", "${memoryUsage} MB", if (memoryUsage > 200) Color.Yellow else Color.Green)
                    StatusItemCustomColor("Network Type", networkType, if (networkType == "None") Color.Red else Color.Green)
                    StatusItemCustomColor("GPS Accuracy", gpsAccuracy, if (gpsAccuracy == "High") Color.Green else Color.Yellow)
                    StatusItemCustomColor("Battery Level", "${currentBattery}%", if (currentBattery > 20) Color.Green else Color.Red)
                    StatusItemCustomColor("Bluetooth State", if (isBluetoothEnabled) "ON" else "OFF", if (isBluetoothEnabled) Color.Green else Color.Red)
                }
            }
"""

parts = content.split("@Composable\nfun StatusItem(title: String, isOk: Boolean, statusText: String) {")
new_content = parts[0].rstrip()[:-1] + mod9_code + "\n        }\n    }\n}\n\n@Composable\nfun StatusItem(title: String, isOk: Boolean, statusText: String) {" + parts[1]

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "w") as f:
    f.write(new_content)
