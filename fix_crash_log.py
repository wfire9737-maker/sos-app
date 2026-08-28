import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

init_old = """  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {"""

init_new = """  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        val stackTrace = android.util.Log.getStackTraceString(throwable)
        getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("last_crash", stackTrace)
            .commit()
        defaultHandler?.uncaughtException(thread, throwable)
    }
    
    val lastCrash = getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)
        .getString("last_crash", null)
    if (lastCrash != null) {
        android.util.Log.e("CRASH_LOG", "Last crash: $lastCrash")
    }

    try {"""

content = content.replace(init_old, init_new)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
