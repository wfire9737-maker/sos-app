import re

with open("app/src/main/java/com/example/ui/screens/VoiceSosScreen.kt", "r") as f:
    content = f.read()

content = content.replace("viewModel.voiceSosService.voiceState", "viewModel.voiceState")
content = content.replace("viewModel.voiceSosService.wakePhrases", "viewModel.wakePhrases")
content = content.replace("viewModel.voiceSosService.micDecibels", "viewModel.micDecibels")
content = content.replace("viewModel.voiceSosService.confidenceThreshold", "viewModel.voiceConfidenceThreshold")
content = content.replace("viewModel.voiceSosService.activationLogs", "viewModel.voiceActivationLogs")
content = content.replace("viewModel.voiceSosService.isSpeechRecognizerActive", "viewModel.isSpeechRecognizerActive")
content = content.replace("viewModel.voiceSosService.liveSpokenText", "viewModel.liveSpokenText")
content = content.replace("viewModel.voiceSosService.speechStatusMessage", "viewModel.speechStatusMessage")
content = content.replace("viewModel.voiceSosService.startSpeechRecognition", "viewModel.voiceSosService.startSpeechRecognition")
content = content.replace("viewModel.voiceSosService.stopSpeechRecognition", "viewModel.voiceSosService.stopSpeechRecognition")

with open("app/src/main/java/com/example/ui/screens/VoiceSosScreen.kt", "w") as f:
    f.write(content)
