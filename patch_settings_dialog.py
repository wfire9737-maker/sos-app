import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

# Add showDeveloperWarningDialog
var_add = "    var showLanguageDialog by remember { mutableStateOf(false) }"
new_var_add = var_add + "\n    var showDeveloperWarningDialog by remember { mutableStateOf(false) }"
content = content.replace(var_add, new_var_add)

# Update switch
old_switch = """
                        onCheckedChange = { enabled -> 
                            viewModel.setDeveloperModeEnabled(enabled)
                            // We can show a snackbar here if we had access to snackbarHostState in this scope, but the about screen also handles it. 
                        }
"""
new_switch = """
                        onCheckedChange = { enabled -> 
                            if (enabled) {
                                showDeveloperWarningDialog = true
                            } else {
                                viewModel.setDeveloperModeEnabled(false)
                            }
                        }
"""
content = content.replace(old_switch, new_switch)

# Add Dialog at the end of the Screen
dialog_code = """
    if (showDeveloperWarningDialog) {
        AlertDialog(
            onDismissRequest = { showDeveloperWarningDialog = false },
            title = { Text("Developer Mode") },
            text = { Text("Warning: Developer mode is intended for testing purposes only and may affect app stability.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setDeveloperModeEnabled(true)
                        showDeveloperWarningDialog = false
                    }
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeveloperWarningDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
"""
# Replace the last `}` of SettingsScreen
content = re.sub(r'}\s*$', dialog_code, content[:content.rfind("}\n\n@Composable\nfun SettingsSection")]) + "\n\n@Composable\nfun SettingsSection" + content.split("\n\n@Composable\nfun SettingsSection")[1]

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)

