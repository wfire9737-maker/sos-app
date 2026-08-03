import re

with open("app/src/main/java/com/example/service/LocationService.kt", "r") as f:
    content = f.read()

old_func = """    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationOnce(timeoutMs: Long = 15000): Location? {
"""
new_func = """    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationOnce(timeoutMs: Long = 15000): Location? {
        if (_isGpsDisabled.value) return null
        if (_isSimulationMode.value) {
            val loc = Location("simulated")
            loc.latitude = _currentLocation.value.latitude
            loc.longitude = _currentLocation.value.longitude
            loc.accuracy = _currentLocation.value.accuracy
            return loc
        }
"""
content = content.replace(old_func, new_func)

with open("app/src/main/java/com/example/service/LocationService.kt", "w") as f:
    f.write(content)
