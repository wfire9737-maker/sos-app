import re

def fix_file(path):
    with open(path, "r") as f:
        content = f.read()
    
    # Remove the broken method body
    content = re.sub(r'    private fun  \{.*?\n    \}\n', '', content, flags=re.DOTALL)
    
    with open(path, "w") as f:
        f.write(content)

fix_file("app/src/main/java/com/example/service/EmergencyHistoryService.kt")
fix_file("app/src/main/java/com/example/service/HistoryService.kt")
