import os

filepath = "app/src/main/java/com/example/repository/EmergencyContactRepositoryImpl.kt"
with open(filepath, "r") as f:
    content = f.read()

# Fix toEntity in insertContact
old_insert = """            contactDao.insertContact(finalContact.toEntity("unknown-uid")) // We need uid here. Let's assume contact model has it. Wait, contact model doesn't have uid.
            Result.success(Unit)"""
new_insert = """            val entity = com.example.data.local.entity.EmergencyContactEntity(
                contactId = finalContact.id,
                uid = finalContact.userId,
                name = finalContact.name,
                phone = finalContact.phone,
                relationship = finalContact.relationship,
                priority = finalContact.priority
            )
            contactDao.insertContact(entity)
            Result.success(Unit)"""
content = content.replace(old_insert, new_insert)

# Fix toContact extension
old_to_contact = """    private fun EmergencyContactEntity.toContact() = EmergencyContact(
        id = this.contactId,
        name = this.name,
        phoneNumber = this.phone,
        relation = this.relationship
    )"""
new_to_contact = """    private fun EmergencyContactEntity.toContact() = EmergencyContact(
        id = this.contactId,
        userId = this.uid,
        name = this.name,
        phone = this.phone,
        relationship = this.relationship,
        priority = this.priority
    )"""
content = content.replace(old_to_contact, new_to_contact)

with open(filepath, "w") as f:
    f.write(content)
