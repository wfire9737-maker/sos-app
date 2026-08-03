import re

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "r") as f:
    content = f.read()

new_custom_gps = """
            if (isSimMode) {
                var customLat by remember { mutableStateOf(currentLocation.latitude.toString()) }
                var customLng by remember { mutableStateOf(currentLocation.longitude.toString()) }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customLat,
                        onValueChange = { customLat = it },
                        label = { Text("Lat") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customLng,
                        onValueChange = { customLng = it },
                        label = { Text("Lng") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { 
                        viewModel.setCustomLocation(currentLocation.latitude, currentLocation.longitude) 
                        customLat = currentLocation.latitude.toString()
                        customLng = currentLocation.longitude.toString()
                    }, modifier = Modifier.weight(1f)) {
                        Text("Use Current")
                    }
                    Button(onClick = { 
                        val lat = customLat.toDoubleOrNull() ?: currentLocation.latitude
                        val lng = customLng.toDoubleOrNull() ?: currentLocation.longitude
                        viewModel.setCustomLocation(lat, lng)
                    }, modifier = Modifier.weight(1f)) {
                        Text("Apply Custom")
                    }
                }
            }
"""

content = re.sub(r"            if \(isSimMode\) \{.*?Custom Lat/Lng \(NY\).*?\}\n                \}\n            \}", new_custom_gps, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "w") as f:
    f.write(content)
