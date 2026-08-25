import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

replacement = """  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        android.util.Log.e("FATAL_ERROR", "Uncaught exception", throwable)
        val stackTrace = throwable.stackTraceToString()
        try {
            val file = java.io.File(getExternalFilesDir(null), "crash_log.txt")
            file.writeText(stackTrace)
        } catch (e: Exception) {}
        
        android.widget.Toast.makeText(this, "Crash: ${throwable.message}", android.widget.Toast.LENGTH_LONG).show()
    }
    
    enableEdgeToEdge()
"""

# Let's just replace the whole onCreate method if possible or the crash handler part
import re
content = re.sub(r'  override fun onCreate\(savedInstanceState: Bundle\?\) \{.*?enableEdgeToEdge\(\)', replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
