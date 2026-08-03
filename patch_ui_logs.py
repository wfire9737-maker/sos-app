import re

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "r") as f:
    content = f.read()

imports = """
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.content.ClipData
import android.content.ClipboardManager
"""
if "import android.content.ClipboardManager" not in content:
    content = content.replace("import android.content.Context", "import android.content.Context\nimport android.content.ClipData\nimport android.content.ClipboardManager\nimport androidx.compose.foundation.lazy.LazyColumn\nimport androidx.compose.foundation.lazy.items")

# Add the UI for Module 8 right before the end of the Scaffold column
mod8_code = """
            Spacer(modifier = Modifier.height(24.dp))
            Text("Module 8 - Developer Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            val devLogs by viewModel.developerLogs.collectAsState()

            // Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.clearDeveloperLogs() }, modifier = Modifier.weight(1f)) {
                    Text("Clear")
                }
                Button(onClick = { 
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val text = devLogs.joinToString("\\n") { "${it.timestamp}: ${it.event} [${it.status}]" }
                    clipboard.setPrimaryClip(ClipData.newPlainText("Developer Logs", text))
                }, modifier = Modifier.weight(1f)) {
                    Text("Copy")
                }
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        val text = devLogs.joinToString("\\n") { "${it.timestamp}: ${it.event} [${it.status}]" }
                        putExtra(Intent.EXTRA_TEXT, text)
                        putExtra(Intent.EXTRA_SUBJECT, "Guardian App Logs")
                    }
                    context.startActivity(Intent.createChooser(intent, "Export Logs"))
                }, modifier = Modifier.weight(1f)) {
                    Text("Export")
                }
            }

            // Simulated events to generate logs for testing
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.addDeveloperLog("Bluetooth Connected", "SUCCESS") }, modifier = Modifier.weight(1f)) {
                    Text("BT Event", style = MaterialTheme.typography.labelSmall)
                }
                Button(onClick = { viewModel.addDeveloperLog("SOS Triggered", "WARNING") }, modifier = Modifier.weight(1f)) {
                    Text("SOS Event", style = MaterialTheme.typography.labelSmall)
                }
                Button(onClick = { viewModel.addDeveloperLog("Network Timeout", "ERROR") }, modifier = Modifier.weight(1f)) {
                    Text("Err Event", style = MaterialTheme.typography.labelSmall)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(devLogs) { log ->
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                        val color = when(log.status) {
                            "ERROR", "CRITICAL" -> Color.Red
                            "WARNING" -> Color.Yellow
                            "SUCCESS" -> Color.Green
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(timeStr, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(70.dp))
                            Text(log.event, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            Text(log.status, style = MaterialTheme.typography.labelSmall, color = color)
                        }
                    }
                }
            }
"""

parts = content.split("@Composable\nfun StatusItem(title: String, isOk: Boolean, statusText: String) {")
new_content = parts[0].rstrip()[:-1] + mod8_code + "\n        }\n    }\n}\n\n@Composable\nfun StatusItem(title: String, isOk: Boolean, statusText: String) {" + parts[1]

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "w") as f:
    f.write(new_content)
