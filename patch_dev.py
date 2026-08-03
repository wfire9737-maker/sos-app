with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "r") as f:
    content = f.read()

import re
content = re.sub(r'android\.bluetooth\.BluetoothAdapter\.getDefaultAdapter\(\)', 
                 r'(context.getSystemService(android.bluetooth.BluetoothManager::class.java)).adapter', 
                 content)

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "w") as f:
    f.write(content)
