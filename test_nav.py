import os

filepath = "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
with open(filepath, "r") as f:
    content = f.read()
    
lines = content.split('\n')
for i, line in enumerate(lines):
    if "HomeBottomNav(" in line:
        print(f"Line {i}: {line}")
        print(f"Line {i+1}: {lines[i+1]}")
        print(f"Line {i+2}: {lines[i+2]}")
        print(f"Line {i+3}: {lines[i+3]}")
