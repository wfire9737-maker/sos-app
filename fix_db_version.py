import os

filepath = "app/src/main/java/com/example/data/local/SmartSosDatabase.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """@Database(
    entities = [
        UserEntity::class,
        EmergencyContactEntity::class,
        SosHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)"""

replacement = """@Database(
    entities = [
        UserEntity::class,
        EmergencyContactEntity::class,
        SosHistoryEntity::class,
        LocationEntity::class
    ],
    version = 2,
    exportSchema = false
)"""

content = content.replace(target, replacement)
with open(filepath, "w") as f:
    f.write(content)
print("Fixed Database version and entities")

# Also update AppModule to add fallbackToDestructiveMigration
app_module_path = "app/src/main/java/com/example/di/AppModule.kt"
with open(app_module_path, "r") as f:
    app_module_content = f.read()

target2 = """.databaseBuilder(
            context,
            SmartSosDatabase::class.java,
            "smart_sos_database"
        ).build()"""

replacement2 = """.databaseBuilder(
            context,
            SmartSosDatabase::class.java,
            "smart_sos_database"
        ).fallbackToDestructiveMigration().build()"""

if target2 in app_module_content:
    app_module_content = app_module_content.replace(target2, replacement2)
    with open(app_module_path, "w") as f:
        f.write(app_module_content)
    print("Added fallbackToDestructiveMigration")
