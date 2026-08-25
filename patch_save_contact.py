import re

with open('app/src/main/java/com/example/service/DatabaseService.kt', 'r') as f:
    content = f.read()

new_save_contact = """    suspend fun saveContact(contact: EmergencyContact): EmergencyContact {
        val finalContact = if (contact.id.isBlank()) {
            contact.copy(id = "contact-" + java.util.UUID.randomUUID().toString().take(8))
        } else {
            contact
        }

        val fs = firestore
        if (fs != null) {
            try {
                runWithRetry {
                    var documentRef = fs.collection("contacts").document(finalContact.id)
                    if (authService != null) {
                        val state = authService.authState.value
                        if (state is com.example.service.AuthState.Success) {
                            documentRef = fs.collection("users").document(state.user.uid)
                                .collection("contacts").document(finalContact.id)
                        }
                    }
                    documentRef.set(finalContact.toMap()).await()
                }
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to save contact on Firestore after multiple attempts, saving locally: ${e.message}")
                saveContactLocally(finalContact)
            }
        } else {
            saveContactLocally(finalContact)
        }

        return finalContact
    }"""

old_save_contact = re.search(r'    suspend fun saveContact\(contact: EmergencyContact\): EmergencyContact \{.*?return finalContact\n    \}', content, re.DOTALL).group(0)

content = content.replace(old_save_contact, new_save_contact)

with open('app/src/main/java/com/example/service/DatabaseService.kt', 'w') as f:
    f.write(content)
