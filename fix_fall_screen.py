import os

filepath = "app/src/main/java/com/example/ui/screens/FallDetectionScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """    if (currentState == "FALL_COUNTDOWN") {
        FallCountdownDialog(
            secondsLeft = countdown,
            onCancel = { viewModel.cancelFallCountdown() }
        )
    }"""

content = content.replace(target, "")

with open(filepath, "w") as f:
    f.write(content)

print("Removed dialog from FallDetectionScreen")
