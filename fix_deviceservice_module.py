import re

with open("app/src/main/java/com/example/di/AppModule.kt", "r") as f:
    content = f.read()

sig_old = "    fun provideDeviceService(@ApplicationContext context: Context, databaseService: DatabaseService, notificationService: NotificationService): DeviceService = DeviceService(context, databaseService, notificationService)"
sig_new = "    fun provideDeviceService(@ApplicationContext context: Context, databaseService: DatabaseService, notificationService: NotificationService, emergencyProvider: EmergencyProvider): DeviceService = DeviceService(context, databaseService, notificationService, emergencyProvider)"

content = content.replace(sig_old, sig_new)

with open("app/src/main/java/com/example/di/AppModule.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/service/DeviceService.kt", "r") as f:
    content = f.read()

class_old = """class DeviceService(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val notificationService: NotificationService
) {"""
class_new = """class DeviceService(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val notificationService: NotificationService,
    private val emergencyProvider: com.example.service.EmergencyProvider
) {"""
content = content.replace(class_old, class_new)

# Update handleIncomingEsp32Sos
handler_old = """    fun handleIncomingEsp32Sos(
        deviceId: String,
        triggerType: String,
        userId: String = "user-101",
        userName: String = "Marcus Vance",
        userPhone: String = "+1-555-0143"
    ) {
        serviceScope.launch {
            addCommLog("🚨 SOS Event received from ESP32 [$deviceId]: Type: $triggerType")
            val device = databaseService.devices.value.find { it.deviceId == deviceId }

            // Update Device Status locally to ALERTing
            device?.let {
                databaseService.updateDevice(it.copy(status = "ALERTing"))
            }

            addCommLog("⏱️ SOS 5-second countdown initiated. Awaiting user cancellation or dispatch...")
        }
    }"""

handler_new = """    fun handleIncomingEsp32Sos(
        deviceId: String,
        triggerType: String,
        userId: String = "user-101",
        userName: String = "Marcus Vance",
        userPhone: String = "+1-555-0143"
    ) {
        serviceScope.launch {
            addCommLog("🚨 SOS Event received from ESP32 [$deviceId]: Type: $triggerType")
            val device = databaseService.devices.value.find { it.deviceId == deviceId }

            // Update Device Status locally to ALERTing
            device?.let {
                databaseService.updateDevice(it.copy(status = "ALERTing"))
            }

            addCommLog("⏱️ SOS 5-second countdown initiated. Awaiting user cancellation or dispatch...")
            emergencyProvider.triggerEmergency(triggerSource = "ESP32_BUTTON", deviceId = deviceId)
        }
    }"""
content = content.replace(handler_old, handler_new)

with open("app/src/main/java/com/example/service/DeviceService.kt", "w") as f:
    f.write(content)
