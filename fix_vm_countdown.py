import os

filepath = "app/src/main/java/com/example/ui/GuardianViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """    val isSirenPlaying = _isSirenPlaying.asStateFlow()

    init {"""
replacement = """    val isSirenPlaying = _isSirenPlaying.asStateFlow()
    
    val countdown: kotlinx.coroutines.flow.StateFlow<Int?> = emergencyService.countdown

    init {"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Added countdown to ViewModel")
else:
    print("Target not found")
