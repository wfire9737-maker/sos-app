import os

filepath = "app/src/main/java/com/example/di/AppModule.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """    @Provides
    @Singleton
    fun provideDatabaseService(@ApplicationContext context: Context): DatabaseService = DatabaseService(context)"""

replacement = """    @Provides
    @Singleton
    fun provideDatabaseService(@ApplicationContext context: Context, database: com.example.data.local.SmartSosDatabase): DatabaseService = DatabaseService(context, database.emergencyContactDao())"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed AppModule for DatabaseService")
else:
    print("Target not found in AppModule")
