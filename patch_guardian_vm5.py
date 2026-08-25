import re

with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'r') as f:
    content = f.read()

bad_block = """        _voiceSosPhrase.value = phrase
        databaseService.saveUserSetting("voice_sos_phrase", phrase)
    } catch (e: Exception) {
            Log.e("GuardianViewModel", "Failed to save voice_sos_phrase: ${e.message}")
        }
    }"""

good_block = """        _voiceSosPhrase.value = phrase
        databaseService.saveUserSetting("voice_sos_phrase", phrase)
    }"""

content = content.replace(bad_block, good_block)

with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'w') as f:
    f.write(content)
