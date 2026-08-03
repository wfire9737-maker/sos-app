import os

filepath = "app/src/main/java/com/example/ui/navigation/NavGraph.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("val fallState by viewModel.fallDetectionState.collectAsState()", "val fallState by viewModel.fallState.collectAsState()")
content = content.replace("onCancel = { viewModel.cancelFallCountdown() }", "onCancel = { viewModel.fallDetectionService.cancelFallCountdown() }")

# Remove the very last '}' character
last_brace_index = content.rfind('}')
if last_brace_index != -1:
    content = content[:last_brace_index] + content[last_brace_index+1:]

with open(filepath, "w") as f:
    f.write(content)

print("Fixed syntax and variables in NavGraph")
