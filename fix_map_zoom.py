import os

filepath = "app/src/main/java/com/example/ui/screens/MapScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """                LaunchedEffect(cameraPositionState.isMoving) {
                    if (cameraPositionState.isMoving && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
                        isCameraPanned = true
                    }
                }"""

replacement = """                LaunchedEffect(cameraPositionState.isMoving) {
                    if (cameraPositionState.isMoving && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
                        isCameraPanned = true
                    }
                }
                
                LaunchedEffect(zoomLevel) {
                    cameraPositionState.animate(CameraUpdateFactory.zoomTo(zoomLevel))
                }"""

content = content.replace(target, replacement, 1)

with open(filepath, "w") as f:
    f.write(content)
