import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

content = content.replace(
    "fun triggerManualSOS(lat: Double? = null, lng: Double? = null) { }",
    """fun triggerManualSOS(lat: Double? = null, lng: Double? = null) {
        emergencyProvider.triggerEmergency(
            triggerSource = "MANUAL_SOS",
            lat = lat,
            lng = lng
        )
    }"""
)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
