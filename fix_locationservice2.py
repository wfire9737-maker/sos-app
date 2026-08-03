import os

filepath = "app/src/main/java/com/example/service/LocationService.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """class LocationService(
    private val context: Context,
    private val firestore: FirebaseFirestore?
) {"""

replacement = """import com.example.data.local.dao.LocationDao
import com.example.data.local.entity.LocationEntity

class LocationService(
    private val context: Context,
    private val firestore: FirebaseFirestore?,
    private val locationDao: LocationDao? = null
) {"""

content = content.replace(target, replacement)

target2 = """    private fun syncLocationToCloud(userLoc: UserLocation) {
        val fs = firestore ?: return
        val uid = userLoc.userId.ifBlank { "anonymous" }
        scope.launch {
            try {
                fs.collection("locations").document(uid).set(userLoc.toMap()).await()
            } catch (e: Exception) {
                Log.e("LocationService", "Cloud location sync failed: ${e.message}")
            }
        }
    }"""

replacement2 = """    private fun syncLocationToCloud(userLoc: UserLocation) {
        val uid = userLoc.userId.ifBlank { "anonymous" }
        scope.launch {
            try {
                locationDao?.insertLocation(
                    LocationEntity(
                        userId = uid,
                        latitude = userLoc.latitude,
                        longitude = userLoc.longitude,
                        speed = userLoc.speed,
                        accuracy = userLoc.accuracy,
                        timestamp = userLoc.timestamp,
                        address = userLoc.address
                    )
                )
            } catch (e: Exception) {
                Log.e("LocationService", "Local DB location sync failed: ${e.message}")
            }
            
            val fs = firestore ?: return@launch
            try {
                fs.collection("locations").document(uid).set(userLoc.toMap()).await()
            } catch (e: Exception) {
                Log.e("LocationService", "Cloud location sync failed: ${e.message}")
            }
        }
    }"""

content = content.replace(target2, replacement2)

with open(filepath, "w") as f:
    f.write(content)
print("Updated LocationService for LocationDao")
