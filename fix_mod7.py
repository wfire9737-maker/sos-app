with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "r") as f:
    content = f.read()

# The error was caused because the insertion was placed after `} }`
# So we have:
#        }
#    }
#
#    Spacer(modifier = ...
# ...
#            }
#        }
#    }
#}
#
#@Composable

# Let's clean it up properly.
import re

# Remove the bad extra brackets at the beginning
content = content.replace("        }\n    }\n\n            Spacer(modifier = Modifier.height(24.dp))", "            Spacer(modifier = Modifier.height(24.dp))")

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "w") as f:
    f.write(content)

