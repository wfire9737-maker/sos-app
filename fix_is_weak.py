import re

with open("app/src/main/java/com/example/service/LocationService.kt", "r") as f:
    content = f.read()

content = re.sub(r'if \(_isWeakGps\.value\) \{[\s\S]*?loc = weakLoc\s*\}', '', content)

with open("app/src/main/java/com/example/service/LocationService.kt", "w") as f:
    f.write(content)
