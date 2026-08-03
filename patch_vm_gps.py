import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

new_methods = """
    // --- MODULE 5: GPS TESTING ---
    val isSimulationMode = locationService.isSimulationMode
    val isGpsDisabled = locationService.isGpsDisabled
    val isWeakGps = locationService.isWeakGps

    fun setSimulationMode(enabled: Boolean) {
        locationService.setSimulationMode(enabled)
    }

    fun setGpsDisabled(disabled: Boolean) {
        locationService.setGpsDisabled(disabled)
    }

    fun setWeakGps(weak: Boolean) {
        locationService.setWeakGps(weak)
    }

    fun setCustomLocation(lat: Double, lng: Double) {
        locationService.setCustomLocation(lat, lng)
    }
"""

content = content.replace("    fun disconnectSimulatedDevice(deviceId: String) {", new_methods + "\n    fun disconnectSimulatedDevice(deviceId: String) {")

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
