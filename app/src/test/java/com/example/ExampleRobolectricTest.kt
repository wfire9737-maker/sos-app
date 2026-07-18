package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.EmergencyContact
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Smart SOS App", appName)
  }

  @Test
  fun `test emergency contact serialization`() {
    val contact = EmergencyContact(
        id = "contact-test-1",
        userId = "user-123",
        name = "John Doe",
        phone = "+1-555-0101",
        relationship = "Spouse",
        priority = 1,
        notes = "Spouse holds spare keys.",
        avatarEmoji = "🚨"
    )

    // Test Map conversion
    val map = contact.toMap()
    assertEquals("contact-test-1", map["id"])
    assertEquals("user-123", map["userId"])
    assertEquals("John Doe", map["name"])
    assertEquals("+1-555-0101", map["phone"])
    assertEquals("Spouse", map["relationship"])
    assertEquals(1, map["priority"])
    assertEquals("Spouse holds spare keys.", map["notes"])
    assertEquals("🚨", map["avatarEmoji"])

    // Test fromMap reconstruction
    val reconstructedFromMap = EmergencyContact.fromMap(map)
    assertEquals(contact, reconstructedFromMap)

    // Test JSON conversion under Robolectric
    val json = contact.toJsonObject()
    assertEquals("contact-test-1", json.getString("id"))
    assertEquals("user-123", json.getString("userId"))
    assertEquals("John Doe", json.getString("name"))
    assertEquals("+1-555-0101", json.getString("phone"))
    assertEquals("Spouse", json.getString("relationship"))
    assertEquals(1, json.getInt("priority"))
    assertEquals("Spouse holds spare keys.", json.getString("notes"))
    assertEquals("🚨", json.getString("avatarEmoji"))

    // Test fromJsonObject reconstruction
    val reconstructedFromJson = EmergencyContact.fromJsonObject(json)
    assertEquals(contact, reconstructedFromJson)
  }

  @Test
  fun `test emergency contact priority sorting`() {
    val contacts = listOf(
        EmergencyContact(name = "Zack", priority = 3),
        EmergencyContact(name = "Alice", priority = 2),
        EmergencyContact(name = "Charlie", priority = 1),
        EmergencyContact(name = "Bob", priority = 2)
    )

    // Sorted by priority first, then name
    val sorted = contacts.sortedWith(compareBy({ it.priority }, { it.name }))

    assertEquals(4, sorted.size)
    // 1. Charlie (Priority 1)
    assertEquals("Charlie", sorted[0].name)
    assertEquals(1, sorted[0].priority)
    // 2. Alice (Priority 2)
    assertEquals("Alice", sorted[1].name)
    assertEquals(2, sorted[1].priority)
    // 3. Bob (Priority 2)
    assertEquals("Bob", sorted[2].name)
    assertEquals(2, sorted[2].priority)
    // 4. Zack (Priority 3)
    assertEquals("Zack", sorted[3].name)
    assertEquals(3, sorted[3].priority)
  }
}
