with open("app/src/main/java/com/example/service/DatabaseService.kt", "r") as f:
    content = f.read()

old_fs = """                val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()"""

new_fs = """                val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build())
                    .build()"""

content = content.replace(old_fs, new_fs)

with open("app/src/main/java/com/example/service/DatabaseService.kt", "w") as f:
    f.write(content)
