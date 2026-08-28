import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

# Replace telemetrySharingEnabled
content = re.sub(
    r'private val _telemetrySharingEnabled = MutableStateFlow\(true\)',
    r'private val _telemetrySharingEnabled = MutableStateFlow(try { getApplication<Application>().getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE).getBoolean("telemetry_sharing", true) } catch(e:Exception) { true })',
    content
)
content = re.sub(
    r'fun setTelemetrySharingEnabled\(enabled: Boolean\) \{ _telemetrySharingEnabled.value = enabled \}',
    r'fun setTelemetrySharingEnabled(enabled: Boolean) { _telemetrySharingEnabled.value = enabled; databaseService.saveUserSetting("telemetry_sharing", enabled) }',
    content
)

# Replace biometricEnabled
content = re.sub(
    r'private val _biometricEnabled = MutableStateFlow\(false\)',
    r'private val _biometricEnabled = MutableStateFlow(try { getApplication<Application>().getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE).getBoolean("biometric_enabled", false) } catch(e:Exception) { false })',
    content
)
content = re.sub(
    r'fun setBiometricEnabled\(enabled: Boolean\) \{ _biometricEnabled.value = enabled \}',
    r'fun setBiometricEnabled(enabled: Boolean) { _biometricEnabled.value = enabled; databaseService.saveUserSetting("biometric_enabled", enabled) }',
    content
)

# Replace appLockPinEnabled
content = re.sub(
    r'private val _appLockPinEnabled = MutableStateFlow\(false\)',
    r'private val _appLockPinEnabled = MutableStateFlow(try { getApplication<Application>().getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE).getBoolean("app_lock_pin_enabled", false) } catch(e:Exception) { false })',
    content
)

# Replace appLockPin
content = re.sub(
    r'private val _appLockPin = MutableStateFlow\(""\)',
    r'private val _appLockPin = MutableStateFlow(try { getApplication<Application>().getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE).getString("app_lock_pin", "") ?: "" } catch(e:Exception) { "" })',
    content
)

# Replace setAppLockPin method
content = re.sub(
    r'fun setAppLockPin\(pin: String\) \{[\s\S]*?\}',
    r'''fun setAppLockPin(pin: String) {
        _appLockPin.value = pin
        _appLockPinEnabled.value = pin.isNotEmpty()
        databaseService.saveUserSetting("app_lock_pin", pin)
        databaseService.saveUserSetting("app_lock_pin_enabled", pin.isNotEmpty())
    }''',
    content
)


with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
