import re

with open('app/src/main/java/com/example/service/DatabaseService.kt', 'r') as f:
    content = f.read()

new_delete_contact = """    suspend fun deleteContact(contactId: String) {
        val fs = firestore
        if (fs != null) {
            try {
                runWithRetry {
                    var documentRef = fs.collection("contacts").document(contactId)
                    if (authService != null) {
                        val state = authService.authState.value
                        if (state is com.example.service.AuthState.Success) {
                            documentRef = fs.collection("users").document(state.user.uid)
                                .collection("contacts").document(contactId)
                        }
                    }
                    documentRef.delete().await()
                }
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to delete contact from Firestore after multiple attempts, updating locally: ${e.message}")
                removeContactLocally(contactId)
            }
        } else {
            removeContactLocally(contactId)
        }
    }"""

old_delete_contact = re.search(r'    suspend fun deleteContact\(contactId: String\) \{.*?removeContactLocally\(contactId\)\n        \}\n    \}', content, re.DOTALL).group(0)

content = content.replace(old_delete_contact, new_delete_contact)

with open('app/src/main/java/com/example/service/DatabaseService.kt', 'w') as f:
    f.write(content)
