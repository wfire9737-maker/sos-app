import os

filepath = "app/src/main/java/com/example/ui/GuardianViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """data class UiState(
    val isLoading: Boolean = false,
    val error: String? = null
)"""

replacement = """data class UiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

data class PermissionsState(
    val locationGranted: Boolean = false,
    val backgroundLocationGranted: Boolean = false,
    val callsGranted: Boolean = false,
    val smsGranted: Boolean = false,
    val contactsGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val audioGranted: Boolean = false,
    val overlayGranted: Boolean = false
)"""

if "PermissionsState" not in content:
    content = content.replace(target, replacement)
    
    target2 = """    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()"""
    
    replacement2 = """    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()
    
    private val _permissionsState = MutableStateFlow(PermissionsState())
    val permissionsState: StateFlow<PermissionsState> = _permissionsState.asStateFlow()
    
    fun refreshPermissions(context: android.content.Context) {
        val location = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val background = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
        val calls = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val sms = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val contacts = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val notifs = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
        val audio = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val overlay = android.provider.Settings.canDrawOverlays(context)
        
        _permissionsState.value = PermissionsState(
            locationGranted = location,
            backgroundLocationGranted = background,
            callsGranted = calls,
            smsGranted = sms,
            contactsGranted = contacts,
            notificationsGranted = notifs,
            audioGranted = audio,
            overlayGranted = overlay
        )
    }"""
    
    content = content.replace(target2, replacement2)
    with open(filepath, "w") as f:
        f.write(content)
    print("Added permissionsState to GuardianViewModel")
else:
    print("PermissionsState already exists")

