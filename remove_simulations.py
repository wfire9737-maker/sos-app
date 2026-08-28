import re

def process_file(file_path):
    with open(file_path, "r") as f:
        content = f.read()

    # Replace populateSimulatedDefaults() with nothing in init/load loops
    content = re.sub(r'if \(list\.isEmpty\(\)\) \{\s*populateSimulatedDefaults\(\)\s*\} else \{\s*_history\.value = list\s*\}', '_history.value = list', content)
    content = re.sub(r'populateSimulatedDefaults\(\)', '', content)

    # Remove the method itself
    content = re.sub(r'private fun populateSimulatedDefaults\(\) \{.*?\n    \}\n', '', content, flags=re.DOTALL)
    
    with open(file_path, "w") as f:
        f.write(content)

process_file("app/src/main/java/com/example/service/EmergencyHistoryService.kt")
process_file("app/src/main/java/com/example/service/HistoryService.kt")
