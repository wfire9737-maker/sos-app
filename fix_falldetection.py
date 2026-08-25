import re

with open('app/src/main/java/com/example/ui/screens/FallDetectionScreen.kt', 'r') as f:
    content = f.read()

pattern = r'            // --- SIMULATE ACCIDENT TRIGGER ---[\s\n]*item \{[\s\n]*Card\([\s\n]*colors = CardDefaults.cardColors\([\s\n]*containerColor = MaterialTheme.colorScheme.errorContainer.copy\(alpha = 0.25f\)[\s\n]*\),[\s\n]*shape = RoundedCornerShape\(16.dp\),[\s\n]*modifier = Modifier.fillMaxWidth\(\)[\s\n]*\) \{[\s\n]*Column\([\s\n]*modifier = Modifier.padding\(14.dp\),[\s\n]*horizontalAlignment = Alignment.CenterHorizontally[\s\n]*\) \{'

content = re.sub(pattern, '', content)

with open('app/src/main/java/com/example/ui/screens/FallDetectionScreen.kt', 'w') as f:
    f.write(content)
