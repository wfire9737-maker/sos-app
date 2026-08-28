import re

with open("app/src/main/java/com/example/ui/screens/VoiceSosScreen.kt", "r") as f:
    content = f.read()

content = content.replace("viewModel.voiceState", "viewModel.voiceSosService.voiceState")
content = content.replace("viewModel.wakePhrases", "viewModel.voiceSosService.wakePhrases")
content = content.replace("viewModel.micDecibels", "viewModel.voiceSosService.micDecibels")
content = content.replace("viewModel.voiceConfidenceThreshold", "viewModel.voiceSosService.confidenceThreshold")
content = content.replace("viewModel.voiceActivationLogs", "viewModel.voiceSosService.activationLogs")
content = content.replace("viewModel.isSpeechRecognizerActive", "viewModel.voiceSosService.isSpeechRecognizerActive")
content = content.replace("viewModel.liveSpokenText", "viewModel.voiceSosService.liveSpokenText")
content = content.replace("viewModel.speechStatusMessage", "viewModel.voiceSosService.speechStatusMessage")
content = content.replace("viewModel.startVoiceRecognition", "viewModel.voiceSosService.startSpeechRecognition")
content = content.replace("viewModel.stopVoiceRecognition", "viewModel.voiceSosService.stopSpeechRecognition")
content = content.replace("viewModel.voiceSosService.voiceSosService", "viewModel.voiceSosService")

with open("app/src/main/java/com/example/ui/screens/VoiceSosScreen.kt", "w") as f:
    f.write(content)
