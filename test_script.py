import re

with open("dummy.kt", "r") as f:
    content = f.read()

# Replace arrivalAlertsEnabled
content = re.sub(
    r'private val _arrivalAlertsEnabled = MutableStateFlow\(true\)',
    r'private val _arrivalAlertsEnabled = MutableStateFlow(try { getApplication<Application>().getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE).getBoolean("arrival_alerts_enabled", true) } catch(e:Exception) { true })',
    content
)
content = re.sub(
    r'fun setArrivalAlertsEnabled\(enabled: Boolean\) \{ _arrivalAlertsEnabled\.value = enabled \}',
    r'fun setArrivalAlertsEnabled(enabled: Boolean) { _arrivalAlertsEnabled.value = enabled; databaseService.saveUserSetting("arrival_alerts_enabled", enabled) }',
    content
)

# Replace deviceStatusNotificationsEnabled
content = re.sub(
    r'private val _deviceStatusNotificationsEnabled = MutableStateFlow\(true\)',
    r'private val _deviceStatusNotificationsEnabled = MutableStateFlow(try { getApplication<Application>().getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE).getBoolean("device_status_notif_enabled", true) } catch(e:Exception) { true })',
    content
)
content = re.sub(
    r'fun setDeviceStatusNotificationsEnabled\(enabled: Boolean\) \{ _deviceStatusNotificationsEnabled\.value = enabled \}',
    r'fun setDeviceStatusNotificationsEnabled(enabled: Boolean) { _deviceStatusNotificationsEnabled.value = enabled; databaseService.saveUserSetting("device_status_notif_enabled", enabled) }',
    content
)

# Replace locationSharingInterval
content = re.sub(
    r'private val _locationSharingInterval = MutableStateFlow\("10s"\)',
    r'private val _locationSharingInterval = MutableStateFlow(try { getApplication<Application>().getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE).getString("location_sharing_interval", "10s") ?: "10s" } catch(e:Exception) { "10s" })',
    content
)
content = re.sub(
    r'fun setLocationSharingInterval\(interval: String\) \{ _locationSharingInterval\.value = interval \}',
    r'fun setLocationSharingInterval(interval: String) { _locationSharingInterval.value = interval; databaseService.saveUserSetting("location_sharing_interval", interval) }',
    content
)

# Replace backgroundLocationEnabled
content = re.sub(
    r'private val _backgroundLocationEnabled = MutableStateFlow\(true\)',
    r'private val _backgroundLocationEnabled = MutableStateFlow(try { getApplication<Application>().getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE).getBoolean("background_location_enabled", true) } catch(e:Exception) { true })',
    content
)
content = re.sub(
    r'fun setBackgroundLocationEnabled\(enabled: Boolean\) \{ _backgroundLocationEnabled\.value = enabled \}',
    r'fun setBackgroundLocationEnabled(enabled: Boolean) { _backgroundLocationEnabled.value = enabled; databaseService.saveUserSetting("background_location_enabled", enabled) }',
    content
)

with open("dummy.kt", "w") as f:
    f.write(content)
