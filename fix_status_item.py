with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "a") as f:
    f.write('''
@Composable
fun StatusItemCustomColor(title: String, statusText: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(statusText, style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}
''')
