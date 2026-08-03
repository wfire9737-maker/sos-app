import re

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "r") as f:
    content = f.read()

net_ui = """
            Spacer(modifier = Modifier.height(24.dp))
            Text("Module 6 - Network & Firebase Testing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            // Firebase status is already rendered at the top, just need buttons.

            val isOfflineMode by viewModel.isOfflineMode.collectAsState()
            val isSlowNetwork by viewModel.isSlowNetwork.collectAsState()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.uploadTestSOS() }, modifier = Modifier.weight(1f)) {
                    Text("Upload Test SOS")
                }
                Button(onClick = { viewModel.downloadTestData() }, modifier = Modifier.weight(1f)) {
                    Text("Download Test Data")
                }
            }

            Button(onClick = { viewModel.deleteTestRecords() }, modifier = Modifier.fillMaxWidth()) {
                Text("Delete Test Records", color = MaterialTheme.colorScheme.error)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isOfflineMode, onCheckedChange = { viewModel.setOfflineMode(it) })
                Text("Simulate Offline Mode")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isSlowNetwork, onCheckedChange = { viewModel.setSlowNetwork(it) })
                Text("Simulate Slow Network")
            }
"""

content = content.replace("            Button(onClick = { \n                val uri = \"geo:${currentLocation.latitude},${currentLocation.longitude}?q=${currentLocation.latitude},${currentLocation.longitude}(Simulated)\"\n                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))\n                try {\n                    context.startActivity(intent)\n                } catch (e: Exception) {}\n            }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {\n                Text(\"Show Google Maps Preview\")\n            }", 
"            Button(onClick = { \n                val uri = \"geo:${currentLocation.latitude},${currentLocation.longitude}?q=${currentLocation.latitude},${currentLocation.longitude}(Simulated)\"\n                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))\n                try {\n                    context.startActivity(intent)\n                } catch (e: Exception) {}\n            }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {\n                Text(\"Show Google Maps Preview\")\n            }\n" + net_ui)

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "w") as f:
    f.write(content)
