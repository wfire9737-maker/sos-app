with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    text = f.read()
    
# split by SettingsSection
screen = text.split("@Composable\nfun SettingsSection")[0]
opens = screen.count("{")
closes = screen.count("}")
print(f"Opens: {opens}, Closes: {closes}")
