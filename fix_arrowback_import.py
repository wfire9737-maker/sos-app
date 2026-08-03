import os

def process_file(filepath):
    with open(filepath, "r") as f:
        content = f.read()
    
    modified = False
    
    target1 = "import androidx.compose.material.icons.automirrored.filled.ArrowBack\n"
    if target1 in content:
        content = content.replace(target1, "")
        modified = True
        
    target2 = "import androidx.compose.material.icons.automirrored.filled.*\n"
    if target2 in content:
        content = content.replace(target2, "")
        modified = True

    if modified:
        with open(filepath, "w") as f:
            f.write(content)
        print(f"Fixed imports in {filepath}")

for root, dirs, files in os.walk("app/src/main/java/com/example/ui"):
    for file in files:
        if file.endswith(".kt"):
            process_file(os.path.join(root, file))

