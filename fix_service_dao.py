import os

filepath = "app/src/main/java/com/example/service/EmergencyService.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """class EmergencyService(
    private val context: Context,
    private val firestore: FirebaseFirestore?,
    private val locationService: LocationService,
    private val notificationService: NotificationService,
    private val databaseService: DatabaseService
) {"""

replacement = """import com.example.data.local.dao.SosHistoryDao
import com.example.data.local.dao.EmergencyContactDao
import com.example.data.local.entity.SosHistoryEntity

class EmergencyService(
    private val context: Context,
    private val firestore: FirebaseFirestore?,
    private val locationService: LocationService,
    private val notificationService: NotificationService,
    private val databaseService: DatabaseService,
    private val sosHistoryDao: SosHistoryDao? = null,
    private val contactDao: EmergencyContactDao? = null
) {"""

if target in content:
    content = content.replace(target, replacement)
    
    target2 = """    private fun saveEmergencyToCloud(model: EmergencyModel) {
        val fs = firestore ?: return
        serviceScope.launch {
            try {
                fs.collection("emergencies").document(model.emergencyId).set(model.toMap()).await()
            } catch (e: Exception) {
                Log.e("EmergencyService", "Failed to sync emergency to Firestore: ${e.message}")
            }
        }
    }"""
    
    replacement2 = """    private fun saveEmergencyToCloud(model: EmergencyModel) {
        serviceScope.launch {
            try {
                val entity = SosHistoryEntity(
                    emergencyId = model.emergencyId,
                    userId = model.userId,
                    triggerSource = model.triggerType,
                    status = model.status,
                    startTime = model.startTimeMs,
                    endTime = model.endTimeMs ?: 0L,
                    latitude = model.latitude,
                    longitude = model.longitude,
                    contactsNotified = model.contactsNotified.size,
                    resolutionNotes = model.notes ?: ""
                )
                sosHistoryDao?.insertHistory(entity)
            } catch (e: Exception) {
                Log.e("EmergencyService", "Failed to save emergency to Room: ${e.message}")
            }
            
            val fs = firestore ?: return@launch
            try {
                fs.collection("emergencies").document(model.emergencyId).set(model.toMap()).await()
            } catch (e: Exception) {
                Log.e("EmergencyService", "Failed to sync emergency to Firestore: ${e.message}")
            }
        }
    }"""
    
    content = content.replace(target2, replacement2)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed EmergencyService with DAOs")
else:
    print("Target not found")
