import re

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "r") as f:
    lines = f.readlines()

# Line 244 is 0-indexed 243
lines[243] = "                onNavigateBack = { navController.navigateUp() },\n" + lines[243]
# Line 275 is 0-indexed 274. Wait, inserting a line shifted the indices!
# It's better to just re-insert using regex on the specific screens.

# Let's write the whole file, finding the specific composable blocks and injecting onNavigateBack
content = "".join(lines)

# Or simply:
content = content.replace("onNavigateToAddPlace = { navController.navigate(Screen.AddEditTrustedPlace.createRoute(null)) },", "onNavigateBack = { navController.navigateUp() },\n                onNavigateToAddPlace = { navController.navigate(Screen.AddEditTrustedPlace.createRoute(null)) },")
content = content.replace("onNavigateToAbout = { navController.navigate(Screen.About.route) },", "onNavigateBack = { navController.navigateUp() },\n                onNavigateToAbout = { navController.navigate(Screen.About.route) },")

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "w") as f:
    f.write(content)

