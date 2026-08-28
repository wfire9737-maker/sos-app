with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "r") as f:
    content = f.read()

bad_block = """                onNavigateBack = { navController.navigateUp() },
                onNavigateToAddPlace = { navController.navigate(Screen.AddEditTrustedPlace.createRoute(null)) },
                onNavigateBack = { navController.navigateUp() },"""

good_block = """                onNavigateBack = { navController.navigateUp() },
                onNavigateToAddPlace = { navController.navigate(Screen.AddEditTrustedPlace.createRoute(null)) },"""

content = content.replace(bad_block, good_block)

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "w") as f:
    f.write(content)
