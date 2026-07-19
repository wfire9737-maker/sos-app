import os

filepath = "app/src/main/java/com/example/ui/navigation/NavGraph.kt"
with open(filepath, "r") as f:
    content = f.read()

old_call = """                onNavigateToHelpFaq = { navController.navigate(Screen.HelpFaq.route) },
                onNavigateToAiScreen = { navController.navigate(Screen.AiScreen.route) }
            )"""
new_call = """                onNavigateToHelpFaq = { navController.navigate(Screen.HelpFaq.route) },
                onNavigateToAiScreen = { navController.navigate(Screen.AiScreen.route) },
                onNavigateToTrustedPlaces = { navController.navigate(Screen.TrustedPlaces.route) },
                onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )"""
content = content.replace(old_call, new_call)

with open(filepath, "w") as f:
    f.write(content)
