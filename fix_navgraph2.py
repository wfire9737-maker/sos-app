import re

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "r") as f:
    lines = f.read().split('\n')

new_lines = []
for line in lines:
    if "else -> {}" not in line and "else -> { }" not in line:
        new_lines.append(line)

out = "\n".join(new_lines)
out = out.replace("is UiEvent.NavigateToEmergency -> {\n                    navController.navigate(Screen.Emergency.route)\n                }", "is UiEvent.NavigateToEmergency -> {\n                    navController.navigate(Screen.Emergency.route)\n                }\n                else -> {}")

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "w") as f:
    f.write(out)
