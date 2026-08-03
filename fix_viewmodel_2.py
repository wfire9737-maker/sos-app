import os

filepath = "app/src/main/java/com/example/ui/GuardianViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """    fun checkSystemReadiness(context: Context): Boolean {
        var isReady = true
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            viewModelScope.launch { _uiEvents.emit(UiEvent.ShowToast("WARNING: GPS is disabled! Location cannot be tracked.")) }
            isReady = false
        }
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo == null || !networkInfo.isConnected) {
            viewModelScope.launch { _uiEvents.emit(UiEvent.ShowToast("WARNING: No Internet! Remote alerts may fail.")) }
            isReady = false
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (!notificationManager.areNotificationsEnabled()) {
            viewModelScope.launch { _uiEvents.emit(UiEvent.ShowToast("WARNING: Notifications are disabled!")) }
            isReady = false
        }
        return isReady
    }"""
replacement = """    fun checkSystemReadiness(): Boolean {
        val context = getApplication<Application>()
        var isReady = true
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            viewModelScope.launch { _uiEvents.emit(UiEvent.ShowToast("WARNING: GPS is disabled! Location cannot be tracked.")) }
            isReady = false
        }
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo == null || !networkInfo.isConnected) {
            viewModelScope.launch { _uiEvents.emit(UiEvent.ShowToast("WARNING: No Internet! Remote alerts may fail.")) }
            isReady = false
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (!notificationManager.areNotificationsEnabled()) {
            viewModelScope.launch { _uiEvents.emit(UiEvent.ShowToast("WARNING: Notifications are disabled!")) }
            isReady = false
        }
        return isReady
    }"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed checkSystemReadiness in ViewModel")
else:
    print("Target not found in ViewModel")
