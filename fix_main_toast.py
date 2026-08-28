import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

init_old = """  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)"""

init_new = """  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    android.widget.Toast.makeText(this, "APP LAUNCHED", android.widget.Toast.LENGTH_LONG).show()"""

content = content.replace(init_old, init_new)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
