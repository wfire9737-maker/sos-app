with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

bad_str = """        )
        if (showDeveloperWarningDialog) {"""

good_str = """        )
    }

    if (showDeveloperWarningDialog) {"""

content = content.replace(bad_str, good_str)

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)
