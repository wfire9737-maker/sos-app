import os

filepath = "app/src/main/java/com/example/service/EmergencyService.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """                val entity = SosHistoryEntity(
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
                )"""

replacement = """                val entity = SosHistoryEntity(
                    historyId = model.emergencyId,
                    uid = model.userId,
                    triggerSource = model.triggerType,
                    status = model.status,
                    date = model.startTimeMs,
                    latitude = model.latitude,
                    longitude = model.longitude,
                    googleMapsLink = "https://maps.google.com/?q=${model.latitude},${model.longitude}"
                )"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed SosHistoryEntity mapping")
else:
    print("Target not found")
