import os
import re

filepath = "app/src/main/java/com/example/ui/screens/MapScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

# MapCanvasEngine function starts with `@Composable\nfun MapCanvasEngine(`
# Let's just find `fun MapCanvasEngine` and remove until the end of the file, since it seems to be at the very end.
target = "fun MapCanvasEngine("
idx = content.find(target)
if idx != -1:
    # Also remove `@Composable` above it
    idx2 = content.rfind("@Composable", 0, idx)
    if idx2 != -1:
        content = content[:idx2]
    
    with open(filepath, "w") as f:
        f.write(content)
    print("Removed MapCanvasEngine")
