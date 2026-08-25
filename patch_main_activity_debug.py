import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {"""

replacement = """  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {
        enableEdgeToEdge()
        setContent {
"""

content = content.replace(target, replacement)

target2 = """          NavGraph(viewModel = guardianViewModel)
        }
      }
    }
  }
}"""

replacement2 = """          NavGraph(viewModel = guardianViewModel)
        }
      }
    }
    } catch (e: Throwable) {
        val stackTrace = android.util.Log.getStackTraceString(e)
        android.util.Log.e("STARTUP_CRASH", "Crash caught in MainActivity: $stackTrace")
        val file = java.io.File(getExternalFilesDir(null), "crash_log.txt")
        file.writeText(stackTrace)
        throw e // Re-throw so we know it crashed, but after logging
    }
  }
}"""

content = content.replace(target2, replacement2)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
