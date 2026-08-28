import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

replacements = [
    (r'databaseService\.getAlerts\(\)', 'databaseService.alerts'),
    (r'databaseService\.getDevices\(\)', 'databaseService.devices'),
    (r'databaseService\.getContacts\(\)', 'databaseService.contacts'),
    (r'trustedPlacesService\.getTrustedPlaces\(\)', 'trustedPlacesService.trustedPlaces'),
    (r'deviceService\.getEsp32CommLogs\(\)', 'deviceService.esp32CommLogs'),
    (r'deviceService\.getDiagnosticsLog\(\)', 'deviceService.diagnosticsLog'),
    (r'notificationService\.getFcmToken\(\)', 'notificationService.fcmToken'),
    (r'historyService\.getHistory\(\)', 'historyService.history'),
    (r'aiAnalysisService\.getAnalysisLogs\(\)', 'aiAnalysisService.analysisLogs'),
    (r'aiAnalysisService\.getCurrentLiveReading\(\)', 'aiAnalysisService.currentLiveReading'),
    (r'aiAnalysisService\.getCurrentLiveAnalysis\(\)', 'aiAnalysisService.currentLiveAnalysis'),
    (r'emergencyService\.getActiveEmergency\(\)', 'emergencyService.activeEmergency'),
    (r'notificationService\.getNotifications\(\)', 'notificationService.notifications'),
    (r'notificationProvider\.getNotifications\(\)', 'notificationProvider.notifications'),
    (r'emergencyService\.getCountdown\(\)', 'emergencyService.countdown'),
    (r'fallDetectionService\.getCurrentState\(\)', 'fallDetectionService.currentState'),
    (r'fallDetectionService\.getCountdownSeconds\(\)', 'fallDetectionService.countdownSeconds'),
    (r'deviceService\.isRefreshing\(\)', 'deviceService.isRefreshing'),
    (r'deviceService\.isEsp32Connected\(\)', 'deviceService.isEsp32Connected'),
    (r'deviceService\.isDiagnosing\(\)', 'deviceService.isDiagnosing'),
    (r'deviceService\.isNetworkAvailable\(\)', 'deviceService.isNetworkAvailable'),
    (r'locationService\.getCurrentLocation\(\)', 'locationService.currentLocation'),
    (r'locationService\.getRoutePoints\(\)', 'locationService.routePoints'),
    (r'locationService\.isTracking\(\)', 'locationService.isTracking'),
    (r'authService\.getAuthState\(\)', 'authService.authState'),
    (r'aiProvider\.getAnalysisLogs\(\)', 'aiProvider.analysisLogs'),
    (r'aiProvider\.getCurrentLiveReading\(\)', 'aiProvider.currentLiveReading'),
    (r'aiProvider\.getCurrentLiveAnalysis\(\)', 'aiProvider.currentLiveAnalysis'),
    (r'deviceService\.getBleManager\(\)\.getMpuHardwareState\(\)', 'deviceService.bleManager.mpuHardwareState'),
    (r'deviceService\.getBleManager\(\)\.getMotionState\(\)', 'deviceService.bleManager.motionState'),
    (r'deviceService\.getBleManager\(\)\.getMpuRawString\(\)', 'deviceService.bleManager.mpuRawString'),
    (r'deviceService\.getBleManager\(\)\.getMpuRecentReadings\(\)', 'deviceService.bleManager.mpuRecentReadings'),
    (r'deviceService\.getBleManager\(\)\.getMpuCharacteristicFound\(\)', 'deviceService.bleManager.mpuCharacteristicFound'),
    (r'deviceService\.getBleManager\(\)\.getMpuNotificationSubscribed\(\)', 'deviceService.bleManager.mpuNotificationSubscribed'),
    (r'deviceService\.getBleManager\(\)\.getLatestMpuReading\(\)', 'deviceService.bleManager.latestMpuReading'),
]

for t, r in replacements:
    content = re.sub(t, r, content)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)

