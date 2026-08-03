import re

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "r") as f:
    content = f.read()

gps_ui = """
            Spacer(modifier = Modifier.height(24.dp))
            Text("Module 5 - GPS Testing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            val isSimMode by viewModel.isSimulationMode.collectAsState()
            val isGpsDisabled by viewModel.isGpsDisabled.collectAsState()
            val isWeakGps by viewModel.isWeakGps.collectAsState()
            val currentLocation by viewModel.currentLocation.collectAsState()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isSimMode, onCheckedChange = { viewModel.setSimulationMode(it) })
                Text("Enable Location Simulation")
            }

            if (isSimMode) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { 
                        viewModel.setCustomLocation(currentLocation.latitude, currentLocation.longitude) // Use current
                    }, modifier = Modifier.weight(1f)) {
                        Text("Use Current Location")
                    }
                    Button(onClick = { 
                        // Simulate generic coordinates (e.g., somewhere in NY)
                        viewModel.setCustomLocation(40.7128, -74.0060)
                    }, modifier = Modifier.weight(1f)) {
                        Text("Custom Lat/Lng (NY)")
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isWeakGps, onCheckedChange = { viewModel.setWeakGps(it) })
                Text("Simulate Weak GPS")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isGpsDisabled, onCheckedChange = { viewModel.setGpsDisabled(it) })
                Text("Simulate GPS Disabled")
            }

            Text("Current Accuracy: ${currentLocation.accuracy}m", style = MaterialTheme.typography.bodyMedium)

            Button(onClick = { 
                val uri = "geo:${currentLocation.latitude},${currentLocation.longitude}?q=${currentLocation.latitude},${currentLocation.longitude}(Simulated)"
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {}
            }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Show Google Maps Preview")
            }
"""

content = content.replace("            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {\n                Button(onClick = {\n                    firstDevice?.let {", gps_ui + "\n            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {\n                Button(onClick = {\n                    firstDevice?.let {")

if "import androidx.compose.material3.Checkbox" not in content:
    content = content.replace("import androidx.compose.material3.*", "import androidx.compose.material3.*\nimport androidx.compose.material3.Checkbox")

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "w") as f:
    f.write(content)
