import os
import re

def process_file(filepath):
    with open(filepath, "r") as f:
        content = f.read()
    
    modified = False
    
    # We want to replace `Icons.Filled.ArrowBack` with `Icons.AutoMirrored.Filled.ArrowBack`, and so on
    # BUT we need to ensure the correct import is present.
    # The simplest fix is actually to just use `Icons.Default.ArrowBack` or `Icons.Filled.ArrowBack`
    # and SUPPRESS the deprecation warnings, because dealing with the AutoMirrored imports across 30 files is messy.
    # Let's revert back to Icons.Filled.X and Icons.Default.X where AutoMirrored is currently used.
    
    patterns = [
        (r"Icons\.AutoMirrored\.Filled\.([A-Za-z0-9_]+)", r"Icons.Filled.\1"),
        (r"import androidx\.compose\.material\.icons\.automirrored\.filled\.[A-Za-z0-9_]+(\n)?", r"")
    ]
    
    for pattern, replacement in patterns:
        if re.search(pattern, content):
            content = re.sub(pattern, replacement, content)
            modified = True

    if modified:
        with open(filepath, "w") as f:
            f.write(content)
        print(f"Reverted {filepath}")

for root, dirs, files in os.walk("app/src/main/java/com/example/ui"):
    for file in files:
        if file.endswith(".kt"):
            process_file(os.path.join(root, file))
