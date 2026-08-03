import re

with open("app/src/main/java/com/example/service/LocationService.kt", "r") as f:
    content = f.read()

# Add simulation state variables
new_vars = """
    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private val _isGpsDisabled = MutableStateFlow(false)
    val isGpsDisabled: StateFlow<Boolean> = _isGpsDisabled.asStateFlow()

    private val _isWeakGps = MutableStateFlow(false)
    val isWeakGps: StateFlow<Boolean> = _isWeakGps.asStateFlow()
"""
content = re.sub(r"    // No simulation mode anymore, so this is just for UI compatibility if needed, but we can keep it false\n    private val _isSimulationMode = MutableStateFlow\(false\)\n    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow\(\)", new_vars, content)

# Update setSimulationMode
new_sim = """
    fun setSimulationMode(enabled: Boolean) {
        _isSimulationMode.value = enabled
        if (!enabled) {
            _isGpsDisabled.value = false
            _isWeakGps.value = false
        }
    }

    fun setGpsDisabled(disabled: Boolean) {
        _isGpsDisabled.value = disabled
    }

    fun setWeakGps(weak: Boolean) {
        _isWeakGps.value = weak
    }

    fun setCustomLocation(lat: Double, lng: Double, accuracy: Float = 5.0f) {
        if (_isSimulationMode.value) {
            val userLoc = _currentLocation.value
            val time = System.currentTimeMillis()
            updateLocationState { 
                it.copy(
                    latitude = lat,
                    longitude = lng,
                    accuracy = accuracy,
                    timestamp = time
                )
            }
        }
    }
"""
content = re.sub(r"    fun setSimulationMode\(enabled: Boolean\) \{\n        _isSimulationMode.value = false\n    \}", new_sim, content)

# Update onLocationResult to handle weak and disabled
old_onloc = "                val loc: Location = locationResult.lastLocation ?: return"
new_onloc = """                if (_isGpsDisabled.value) return
                var loc: Location = locationResult.lastLocation ?: return
                
                if (_isSimulationMode.value) return // Don't use real GPS if in generic sim mode (custom location)

                var lat = loc.latitude
                var lng = loc.longitude
                var acc = loc.accuracy

                if (_isWeakGps.value) {
                    // Inject jitter and high inaccuracy
                    lat += (Math.random() - 0.5) * 0.005
                    lng += (Math.random() - 0.5) * 0.005
                    acc = 250f + (Math.random() * 200).toFloat()
                    
                    val weakLoc = Location(loc)
                    weakLoc.latitude = lat
                    weakLoc.longitude = lng
                    weakLoc.accuracy = acc
                    loc = weakLoc
                }
"""
content = content.replace(old_onloc, new_onloc)


# Also in getCurrentLocationOnce, we need to respect these flags.
with open("app/src/main/java/com/example/service/LocationService.kt", "w") as f:
    f.write(content)
