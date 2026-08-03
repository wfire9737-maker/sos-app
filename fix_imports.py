import os

def process_file(filepath):
    with open(filepath, "r") as f:
        content = f.read()
    
    if "import androidx.compose.material.icons.automirrored.filled.*" not in content:
        target = "import androidx.compose.material.icons.filled.*"
        if target in content:
            content = content.replace(target, "import androidx.compose.material.icons.filled.*\nimport androidx.compose.material.icons.automirrored.filled.*")
            with open(filepath, "w") as f:
                f.write(content)
            print(f"Updated imports in {filepath}")

for root, dirs, files in os.walk("app/src/main/java/com/example/ui/screens"):
    for file in files:
        if file.endswith(".kt"):
            process_file(os.path.join(root, file))

