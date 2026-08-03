import os
import re

filepath = "app/src/main/java/com/example/di/AppModule.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """    @Provides
    @Singleton
    fun provideEmergencyService(@ApplicationContext context: Context, databaseService: DatabaseService, locationService: LocationService, notificationService: NotificationService): EmergencyService = EmergencyService(context, databaseService.firestoreInstance, locationService, notificationService, databaseService)"""

replacement = """    @Provides
    @Singleton
    fun provideEmergencyService(
        @ApplicationContext context: Context, 
        databaseService: DatabaseService, 
        locationService: LocationService, 
        notificationService: NotificationService,
        database: com.example.data.local.SmartSosDatabase
    ): EmergencyService = EmergencyService(
        context, 
        databaseService.firestoreInstance, 
        locationService, 
        notificationService, 
        databaseService,
        database.sosHistoryDao(),
        database.emergencyContactDao()
    )"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed AppModule")
else:
    print("Target not found")
