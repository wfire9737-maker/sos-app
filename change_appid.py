import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

content = re.sub(r'applicationId\s*=\s*"[^"]+"', 'applicationId = "com.example.smartsos"', content)

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
