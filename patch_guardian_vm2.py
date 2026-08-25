import re

with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'r') as f:
    content = f.read()

# Fix the syntax error in setSosSoundEnabled
bad_block = """    fun setSosSoundEnabled(enabled: Boolean) {
        _sosSoundEnabled.value = enabled
        databaseService.saveUserSetting("sos_sound_enabled", enabled)
    } catch (e: Exception) {
            Log.e("GuardianViewModel", "Failed to save sos_sound_enabled: ${e.message}")
        }
    }"""

good_block = """    fun setSosSoundEnabled(enabled: Boolean) {
        _sosSoundEnabled.value = enabled
        databaseService.saveUserSetting("sos_sound_enabled", enabled)
    }"""

content = content.replace(bad_block, good_block)

with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'w') as f:
    f.write(content)
