import os

filepath = "app/src/main/java/com/example/ui/screens/AIScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

# Add activity recognition
old_ai = """        item {
            Card(
                modifier = Modifier.fillMaxWidth(),"""
new_ai = """        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Activity Recognition", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Current Activity: Walking", style = MaterialTheme.typography.bodyLarge)
                    Text("False Alarm Probability: 5%", style = MaterialTheme.typography.bodyMedium, color = SafetyGreen)
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),"""
content = content.replace(old_ai, new_ai)

with open(filepath, "w") as f:
    f.write(content)
