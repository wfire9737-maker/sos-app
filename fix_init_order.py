import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

# Extract the init block
init_pattern = r'init\s*\{.*?\n    \}\n'
init_match = re.search(init_pattern, content, re.DOTALL)
if init_match:
    init_block = init_match.group(0)
    # Remove it from the current position
    content = content.replace(init_block, '')
    
    # Place it after voiceSosPhrase
    target_anchor = r'val voiceSosPhrase = _voiceSosPhrase\.asStateFlow\(\)\n'
    content = re.sub(target_anchor, target_anchor + "\n    " + init_block, content)
    
    with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
        f.write(content)
        print("Success")
else:
    print("Init block not found")
