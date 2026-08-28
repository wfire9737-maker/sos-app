import re

with open("app/src/main/java/com/example/ui/screens/EmergencyScreen.kt", "r") as f:
    content = f.read()

content = content.replace("import com.example.model.SosWorkflowState", "import com.example.model.SosWorkflowState\nimport com.example.model.EmergencyModel")

state_old = "    val sosWorkflowState by viewModel.sosWorkflowState.collectAsState()"
state_new = "    val sosWorkflowState by viewModel.sosWorkflowState.collectAsState()\n    val activeEmergency by viewModel.activeEmergency.collectAsState()"

content = content.replace(state_old, state_new)

with open("app/src/main/java/com/example/ui/screens/EmergencyScreen.kt", "w") as f:
    f.write(content)
