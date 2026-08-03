with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

cleanup_old = """    override fun onCleared() {
        super.onCleared()
        locationService.stopTracking()
    }"""
    
cleanup_new = """    override fun onCleared() {
        super.onCleared()
        locationService.stopTracking()
        deviceService.cleanup()
    }"""
    
content = content.replace(cleanup_old, cleanup_new)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
