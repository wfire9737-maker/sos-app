with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "r") as f:
    lines = f.readlines()

out = []
for i, line in enumerate(lines):
    if "composable(Screen.DeveloperDashboard.route) {" in line:
        if "}" not in lines[i-1]:
            out.append("        }\n")
    out.append(line)

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "w") as f:
    f.writelines(out)
