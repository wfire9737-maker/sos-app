import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

init_old = """    val lastCrash = getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)
        .getString("last_crash", null)
    if (lastCrash != null) {
        android.util.Log.e("CRASH_LOG", "Last crash: $lastCrash")
    }

    try {
        enableEdgeToEdge()
        setContent {
          val guardianViewModel: GuardianViewModel = androidx.hilt.navigation.compose.hiltViewModel()"""

init_new = """    val lastCrash = getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)
        .getString("last_crash", null)
    if (lastCrash != null) {
        android.util.Log.e("CRASH_LOG", "Last crash: $lastCrash")
    }

    try {
        enableEdgeToEdge()
        setContent {
          if (lastCrash != null) {
             androidx.compose.foundation.layout.Box(
                 modifier = androidx.compose.ui.Modifier.fillMaxSize().androidx.compose.foundation.background(androidx.compose.ui.graphics.Color.Red)
             ) {
                 androidx.compose.foundation.lazy.LazyColumn {
                     item {
                         androidx.compose.material3.Text(
                             text = lastCrash,
                             color = androidx.compose.ui.graphics.Color.White,
                             modifier = androidx.compose.ui.Modifier.padding(16.dp)
                         )
                     }
                 }
             }
             return@setContent
          }
          val guardianViewModel: GuardianViewModel = androidx.hilt.navigation.compose.hiltViewModel()"""

content = content.replace(init_old, init_new)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
