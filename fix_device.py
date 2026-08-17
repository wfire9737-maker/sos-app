import re

file_path = "app/src/main/java/com/example/service/DeviceService.kt"

with open(file_path, "r") as f:
    content = f.read()

# Fix import
if "import kotlinx.coroutines.flow.asSharedFlow" not in content:
    content = content.replace("import kotlinx.coroutines.flow.asStateFlow", "import kotlinx.coroutines.flow.asStateFlow\nimport kotlinx.coroutines.flow.asSharedFlow")

# Move startEsp32Polling inside the class
polling_func = """    private fun startEsp32Polling() {"""
index = content.find(polling_func)
if index != -1:
    content_before = content[:index]
    content_after = content[index:]
    
    # Remove the extra `}` that closes the class before startEsp32Polling
    content_before = content_before.rstrip()
    if content_before.endswith("}"):
        content_before = content_before[:-1]
    
    content = content_before + "\n" + content_after + "\n}"

with open(file_path, "w") as f:
    f.write(content)

print("Fixed DeviceService.kt")
