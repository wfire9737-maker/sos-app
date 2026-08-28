import re

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    content = f.read()

import_block = """import com.example.model.SosWorkflowState
import com.example.model.User
import com.example.ui.rememberLocationPermissionHandler"""

import_replacement = """import com.example.model.SosWorkflowState
import com.example.model.User
import com.example.ui.rememberLocationPermissionHandler
import com.example.ble.BleManager.BleState"""

content = content.replace(import_block, import_replacement)

# Update HomeScreen signature/vars
home_vars_old = """    val authState by viewModel.authState.collectAsState()
    val isEsp32Connected by viewModel.isEsp32Connected.collectAsState()
    val sosWorkflowState by viewModel.sosWorkflowState.collectAsState()"""

home_vars_new = """    val authState by viewModel.authState.collectAsState()
    val isEsp32Connected by viewModel.isEsp32Connected.collectAsState()
    val sosWorkflowState by viewModel.sosWorkflowState.collectAsState()
    val bleBatteryDisplay by viewModel.bleBatteryDisplay.collectAsState()
    val bleConnectionState by viewModel.bleConnectionState.collectAsState()"""

content = content.replace(home_vars_old, home_vars_new)

# Update StitchDeviceCard call
card_old = """            StitchDeviceCard(
                isConnected = isEsp32Connected,
                onClick = onNavigateToDevicePairing
            )"""

card_new = """            StitchDeviceCard(
                isConnected = isEsp32Connected,
                batteryText = bleBatteryDisplay,
                bleState = bleConnectionState,
                onClick = onNavigateToDevicePairing
            )"""

content = content.replace(card_old, card_new)

# Update StitchDeviceCard definition
def_old = """@Composable
fun StitchDeviceCard(isConnected: Boolean, onClick: () -> Unit) {"""

def_new = """@Composable
fun StitchDeviceCard(isConnected: Boolean, batteryText: String, bleState: BleState, onClick: () -> Unit) {"""

content = content.replace(def_old, def_new)

sync_old = """                    Text(
                        text = if (isConnected) "Last synced: Just now" else "Disconnected",
                        color = StitchTextMuted,
                        fontSize = 12.sp
                    )"""

sync_new = """                    Text(
                        text = when (bleState) {
                            BleState.CONNECTED, BleState.READY -> "Connected"
                            BleState.DISCONNECTED -> "Disconnected"
                            BleState.SCANNING -> "Scanning..."
                            BleState.CONNECTING, BleState.DISCOVERING_SERVICES -> "Connecting..."
                            else -> "Connecting..."
                        },
                        color = StitchTextMuted,
                        fontSize = 12.sp
                    )"""

content = content.replace(sync_old, sync_new)

battery_old = """                Text("88%", color = Color.White, fontSize = 10.sp)"""

battery_new = """                Text(batteryText.ifBlank { "N/A" }, color = Color.White, fontSize = 10.sp)"""

content = content.replace(battery_old, battery_new)

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(content)
