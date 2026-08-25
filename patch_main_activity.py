import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

replacement = """        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        try {
            if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                promptEnableLocation()
            }
        } catch (e: Exception) {
            // Ignore
        }"""

content = content.replace("""        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            promptEnableLocation()
        }""", replacement)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
