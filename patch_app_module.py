import re

with open('app/src/main/java/com/example/di/AppModule.kt', 'r') as f:
    content = f.read()

replacement = """    @Provides
    @Singleton
    fun provideFirebaseFirestore(@ApplicationContext context: Context): com.google.firebase.firestore.FirebaseFirestore? {
        return try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }"""

content = re.sub(r'    @Provides\s+@Singleton\s+fun provideFirebaseFirestore\(@ApplicationContext context: Context\): com\.google\.firebase\.firestore\.FirebaseFirestore = com\.google\.firebase\.firestore\.FirebaseFirestore\.getInstance\(\)', replacement, content, flags=re.MULTILINE)

with open('app/src/main/java/com/example/di/AppModule.kt', 'w') as f:
    f.write(content)
