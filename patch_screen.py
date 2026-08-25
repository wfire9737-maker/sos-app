with open('app/src/main/java/com/example/ui/navigation/Screen.kt', 'r') as f:
    content = f.read()

content = content.replace('object DeveloperDashboard : Screen("developer_dashboard")', 'object DeveloperDashboard : Screen("developer_dashboard")\n    object BleTest : Screen("ble_test")')

with open('app/src/main/java/com/example/ui/navigation/Screen.kt', 'w') as f:
    f.write(content)
