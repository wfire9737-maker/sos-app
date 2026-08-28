import re

with open("app/src/main/java/com/example/service/EmergencyProvider.kt", "r") as f:
    content = f.read()

# Fix triggerEmergency
content = content.replace(
    "customLat = lat ?: locationService.currentLocation.value.latitude",
    "customLat = lat"
)
content = content.replace(
    "customLng = lng ?: locationService.currentLocation.value.longitude",
    "customLng = lng"
)
content = content.replace(
    "customAccuracy = accuracy ?: locationService.currentLocation.value.accuracy",
    "customAccuracy = accuracy"
)

with open("app/src/main/java/com/example/service/EmergencyProvider.kt", "w") as f:
    f.write(content)
