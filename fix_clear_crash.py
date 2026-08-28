import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

init_old = """                         Text(
                             text = lastCrash,
                             color = Color.White,
                             modifier = Modifier.padding(16.dp)
                         )
                     }"""

init_new = """                         Text(
                             text = lastCrash,
                             color = Color.White,
                             modifier = Modifier.padding(16.dp)
                         )
                     }
                     item {
                         androidx.compose.material3.Button(onClick = {
                             getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)
                                .edit()
                                .remove("last_crash")
                                .commit()
                         }, modifier = Modifier.padding(16.dp)) {
                             Text("Clear Crash Log")
                         }
                     }"""
content = content.replace(init_old, init_new)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
