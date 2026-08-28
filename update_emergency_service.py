import re

with open("app/src/main/java/com/example/service/EmergencyService.kt", "r") as f:
    content = f.read()

# Fix Call logic
call_pattern = r'val phoneToCall = primaryContact\?\.phone \?: "911"\s*databaseService\.addDeveloperLog\("CALL_REQUESTED: \$phoneToCall \(ID: \$emergencyId\)", "INFO"\)\s*if \(lastCalledEmergencyId == emergencyId\) \{[\s\S]*?Log\.w\("EmergencyService", "CALL_PERMISSION_DENIED: Cannot place call\."\)\s*\}\s*\}\s*\}'

new_call_logic = """val phoneToCall = primaryContact?.phone
                if (phoneToCall.isNullOrEmpty()) {
                    databaseService.addDeveloperLog("CALL_SKIPPED: No emergency contacts configured", "INFO")
                    Log.w("EmergencyService", "CALL_SKIPPED: No emergency contacts configured")
                } else {
                    databaseService.addDeveloperLog("CALL_REQUESTED: $phoneToCall (ID: $emergencyId)", "INFO")
                    if (lastCalledEmergencyId == emergencyId) {
                        Log.w("EmergencyService", "Call already placed for emergency: $emergencyId")
                    } else {
                        lastCalledEmergencyId = emergencyId
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                            Log.d("EmergencyService", "CALL_REQUESTED: Attempting background dial to $phoneToCall")
                            val callIntent = Intent(Intent.ACTION_CALL).apply {
                                data = Uri.parse("tel:$phoneToCall")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try {
                                context.startActivity(callIntent)
                                databaseService.addDeveloperLog("CALL_STARTED: tel:$phoneToCall", "SUCCESS")
                                Log.d("EmergencyService", "CALL_STARTED: Successfully launched dialer activity.")
                            } catch (e: Exception) {
                                databaseService.addDeveloperLog("CALL_FAILED: ${e.message}", "ERROR")
                                Log.e("EmergencyService", "CALL_FAILED: Failed to start background call activity: ${e.message}")
                            }
                        } else {
                            databaseService.addDeveloperLog("CALL_PERMISSION_DENIED: CALL_PHONE permission not granted", "ERROR")
                            Log.w("EmergencyService", "CALL_PERMISSION_DENIED: Cannot place call.")
                        }
                    }
                }"""

content = re.sub(call_pattern, new_call_logic, content)

# Fix SMS logic
sms_pattern = r'try \{\s*// For long SMS, we should use sendMultipartTextMessage\s*val parts = smsManager\?\.divideMessage\(message\)\s*if \(parts != null\) \{\s*smsManager\.sendMultipartTextMessage\(contact\.phone, null, parts, null, null\)\s*\} else \{\s*smsManager\?\.sendTextMessage\(contact\.phone, null, message, null, null\)\s*\}\s*\} catch \(e: Exception\) \{\s*Log\.e\("EmergencyService", "Failed to send real SMS to \$\{contact\.phone\}: \$\{e\.message\}"\)\s*\}'

new_sms_logic = """if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                try {
                    // For long SMS, we should use sendMultipartTextMessage
                    val parts = smsManager?.divideMessage(message)
                    if (parts != null) {
                        smsManager.sendMultipartTextMessage(contact.phone, null, parts, null, null)
                    } else {
                        smsManager?.sendTextMessage(contact.phone, null, message, null, null)
                    }
                } catch (e: Exception) {
                    Log.e("EmergencyService", "Failed to send real SMS to ${contact.phone}: ${e.message}")
                }
            } else {
                Log.w("EmergencyService", "SMS_PERMISSION_DENIED: Cannot send SMS.")
            }"""

content = re.sub(sms_pattern, new_sms_logic, content)

with open("app/src/main/java/com/example/service/EmergencyService.kt", "w") as f:
    f.write(content)
