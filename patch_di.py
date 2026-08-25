import re

with open('app/src/main/java/com/example/di/AppModule.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'fun provideDatabaseService(@ApplicationContext context: Context, database: com.example.data.local.SmartSosDatabase): DatabaseService = DatabaseService(context, database.emergencyContactDao())',
    'fun provideDatabaseService(@ApplicationContext context: Context, database: com.example.data.local.SmartSosDatabase, authService: AuthService): DatabaseService = DatabaseService(context, authService, database.emergencyContactDao())'
)

with open('app/src/main/java/com/example/di/AppModule.kt', 'w') as f:
    f.write(content)
