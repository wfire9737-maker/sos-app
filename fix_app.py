import re

with open("app/src/main/java/com/example/SmartSOSApplication.kt", "r") as f:
    content = f.read()

init_old = """class SmartSOSApplication : Application() {
    override fun onCreate() {
        super.onCreate()"""

init_new = """class SmartSOSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stackTrace = android.util.Log.getStackTraceString(throwable)
            getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString("last_crash", stackTrace)
                .commit()
            defaultHandler?.uncaughtException(thread, throwable)
        }"""
content = content.replace(init_old, init_new)

with open("app/src/main/java/com/example/SmartSOSApplication.kt", "w") as f:
    f.write(content)
