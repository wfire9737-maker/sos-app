import re

with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'r') as f:
    content = f.read()

# I will replace direct shared pref updates with databaseService.saveUserSetting()

new_set_sos_sound = """    fun setSosSoundEnabled(enabled: Boolean) {
        _sosSoundEnabled.value = enabled
        databaseService.saveUserSetting("sos_sound_enabled", enabled)
    }"""
content = re.sub(r'    fun setSosSoundEnabled\(enabled: Boolean\).*?    \}', new_set_sos_sound, content, flags=re.DOTALL)

new_set_voice_sos = """    fun setVoiceSosEnabled(enabled: Boolean) {
        _voiceSosEnabled.value = enabled
        databaseService.saveUserSetting("voice_sos_enabled", enabled)
        voiceSosService.toggleListening(enabled)
    }"""
content = re.sub(r'    fun setVoiceSosEnabled\(enabled: Boolean\).*?voiceSosService\.toggleListening\(enabled\)\n    \}', new_set_voice_sos, content, flags=re.DOTALL)

new_set_voice_phrase = """    fun setVoiceSosPhrase(phrase: String) {
        _voiceSosPhrase.value = phrase
        databaseService.saveUserSetting("voice_sos_phrase", phrase)
    }"""
content = re.sub(r'    fun setVoiceSosPhrase\(phrase: String\).*?    \}', new_set_voice_phrase, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'w') as f:
    f.write(content)
