import os

filepath = "app/src/main/java/com/example/ui/screens/MapScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF44474E).copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))"""

replacement = """            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF44474E).copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))
            
            // Address & Coordinates
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (location.address.isNotEmpty()) location.address else "Acquiring Address...",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${String.format("%.5f", location.latitude)}, ${String.format("%.5f", location.longitude)}",
                    color = Color(0xFFA0B0C0),
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed TelemetryInstrumentHUD")
else:
    print("Target not found")
