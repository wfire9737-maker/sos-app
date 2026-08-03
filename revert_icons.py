import os

icons_to_replace = [
    "InsertDriveFile", "Sort", "DirectionsRun", "VolumeOff", "VolumeUp", 
    "ArrowForward", "DirectionsWalk", "ArrowBack", "HelpOutline", "ExitToApp", "Help"
]

def process_file(filepath):
    with open(filepath, "r") as f:
        content = f.read()
    
    modified = False
    for icon in icons_to_replace:
        target = f"Icons.AutoMirrored.Filled.{icon}"
        replacement = f"Icons.Filled.{icon}"
        if target in content:
            content = content.replace(target, replacement)
            modified = True
            
    if modified:
        with open(filepath, "w") as f:
            f.write(content)
        print(f"Reverted {filepath}")

for root, dirs, files in os.walk("app/src/main/java/com/example/ui"):
    for file in files:
        if file.endswith(".kt"):
            process_file(os.path.join(root, file))

