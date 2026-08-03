import os

filepath = "app/src/main/java/com/example/service/DatabaseService.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """class DatabaseService(private val context: Context) {"""
replacement = """import com.example.data.local.dao.EmergencyContactDao
import com.example.data.local.entity.EmergencyContactEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DatabaseService(private val context: Context, private val contactDao: EmergencyContactDao? = null) {"""

if target in content:
    content = content.replace(target, replacement)
    
    target2 = """    private fun saveContactLocally(contact: EmergencyContact) {
        val currentList = _contacts.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == contact.id }
        if (index >= 0) {
            currentList[index] = contact
        } else {
            currentList.add(contact)
        }
        saveContactsListLocally(currentList)
    }"""
    
    replacement2 = """    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun saveContactLocally(contact: EmergencyContact) {
        val currentList = _contacts.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == contact.id }
        if (index >= 0) {
            currentList[index] = contact
        } else {
            currentList.add(contact)
        }
        saveContactsListLocally(currentList)
        
        // Save to Room
        scope.launch {
            try {
                val entity = EmergencyContactEntity(
                    contactId = contact.id,
                    uid = contact.userId,
                    name = contact.name,
                    phone = contact.phone,
                    relationship = contact.relationship,
                    priority = contact.priority
                )
                contactDao?.insertContact(entity)
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to save contact to Room", e)
            }
        }
    }"""
    
    content = content.replace(target2, replacement2)
    
    target3 = """    private fun removeContactLocally(contactId: String) {
        val currentList = _contacts.value.filter { it.id != contactId }
        saveContactsListLocally(currentList)
    }"""
    
    replacement3 = """    private fun removeContactLocally(contactId: String) {
        val currentList = _contacts.value.filter { it.id != contactId }
        saveContactsListLocally(currentList)
        
        scope.launch {
            try {
                contactDao?.deleteContact(contactId)
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to delete contact from Room", e)
            }
        }
    }"""
    
    content = content.replace(target3, replacement3)
    
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed DatabaseService with Room")
else:
    print("Target not found in DatabaseService")
