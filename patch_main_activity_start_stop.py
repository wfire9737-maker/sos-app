import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

target = """            try {
                context.startService(android.content.Intent(context, com.example.service.NearbyBleService::class.java))
            } catch(e: Exception) {
                android.util.Log.e("MainActivity", "Failed to start NearbyBleService", e)
            }"""
replacement = "            com.example.service.NearbyBleService.startOrStop(context)"

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
