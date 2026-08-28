import re

with open("app/src/main/java/com/example/service/GuardianFirebaseMessagingService.kt", "r") as f:
    content = f.read()

content = content.replace(
    "NotificationService(applicationContext, dbService.firestoreInstance)",
    "NotificationService(applicationContext, dbService.firestoreInstance, com.example.data.SettingsDataStore(applicationContext))"
)

with open("app/src/main/java/com/example/service/GuardianFirebaseMessagingService.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/di/AppModule.kt", "r") as f:
    content = f.read()

content = content.replace(
    "fun provideNotificationService(@ApplicationContext context: Context, databaseService: DatabaseService): NotificationService = NotificationService(context, databaseService.firestoreInstance)",
    "fun provideNotificationService(@ApplicationContext context: Context, databaseService: DatabaseService, settingsDataStore: com.example.data.SettingsDataStore): NotificationService = NotificationService(context, databaseService.firestoreInstance, settingsDataStore)"
)

with open("app/src/main/java/com/example/di/AppModule.kt", "w") as f:
    f.write(content)

