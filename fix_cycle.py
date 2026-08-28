import re

with open("app/src/main/java/com/example/di/AppModule.kt", "r") as f:
    content = f.read()

sig_old = "    fun provideDeviceService(@ApplicationContext context: Context, databaseService: DatabaseService, notificationService: NotificationService, emergencyProvider: EmergencyProvider): DeviceService = DeviceService(context, databaseService, notificationService, emergencyProvider)"
sig_new = "    fun provideDeviceService(@ApplicationContext context: Context, databaseService: DatabaseService, notificationService: NotificationService, emergencyProvider: dagger.Lazy<EmergencyProvider>): DeviceService = DeviceService(context, databaseService, notificationService, emergencyProvider)"
content = content.replace(sig_old, sig_new)

with open("app/src/main/java/com/example/di/AppModule.kt", "w") as f:
    f.write(content)


with open("app/src/main/java/com/example/service/DeviceService.kt", "r") as f:
    content = f.read()

class_old = """    private val notificationService: NotificationService,
    private val emergencyProvider: com.example.service.EmergencyProvider
) {"""

class_new = """    private val notificationService: NotificationService,
    private val emergencyProvider: dagger.Lazy<com.example.service.EmergencyProvider>
) {"""
content = content.replace(class_old, class_new)

handler_old = "            emergencyProvider.triggerEmergency(triggerSource = \"ESP32_BUTTON\", deviceId = deviceId)"
handler_new = "            emergencyProvider.get().triggerEmergency(triggerSource = \"ESP32_BUTTON\", deviceId = deviceId)"
content = content.replace(handler_old, handler_new)

with open("app/src/main/java/com/example/service/DeviceService.kt", "w") as f:
    f.write(content)
