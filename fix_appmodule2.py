import os

filepath = "app/src/main/java/com/example/di/AppModule.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("fun provideLocationService(@ApplicationContext context: Context, databaseService: DatabaseService): LocationService = LocationService(context, databaseService.firestoreInstance)", "fun provideLocationService(@ApplicationContext context: Context, databaseService: DatabaseService, database: com.example.data.local.SmartSosDatabase): LocationService = LocationService(context, databaseService.firestoreInstance, database.locationDao())")

with open(filepath, "w") as f:
    f.write(content)
print("Updated AppModule for LocationDao")
