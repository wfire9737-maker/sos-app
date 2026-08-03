with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "r") as f:
    text = f.read()

parts = text.split("@Composable\nfun StatusItem")

part1 = parts[0]
part2 = parts[1]

# Balance part1
count = 0
for c in part1:
    if c == '{': count += 1
    elif c == '}': count -= 1

print(f"Balance before StatusItem: {count}")

while count < 0:
    idx = part1.rfind('}')
    part1 = part1[:idx] + part1[idx+1:]
    count += 1
    
while count > 0:
    part1 += "}\n"
    count -= 1

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "w") as f:
    f.write(part1 + "\n@Composable\nfun StatusItem" + part2)
