import os

filepath = "app/src/main/java/com/example/data/local/SmartSosDatabase.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("import com.example.data.local.entity.EmergencyContactEntity", "import com.example.data.local.entity.EmergencyContactEntity\nimport com.example.data.local.entity.LocationEntity\nimport com.example.data.local.dao.LocationDao")

content = content.replace("entities = [UserEntity::class, SosHistoryEntity::class, EmergencyContactEntity::class]", "entities = [UserEntity::class, SosHistoryEntity::class, EmergencyContactEntity::class, LocationEntity::class]")

content = content.replace("abstract fun emergencyContactDao(): EmergencyContactDao", "abstract fun emergencyContactDao(): EmergencyContactDao\n    abstract fun locationDao(): LocationDao")

with open(filepath, "w") as f:
    f.write(content)
print("Updated SmartSosDatabase")
