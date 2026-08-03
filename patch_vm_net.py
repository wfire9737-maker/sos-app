import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

new_methods = """
    // --- MODULE 6: NETWORK & FIREBASE TESTING ---
    val isOfflineMode = databaseService.isOfflineMode
    val isSlowNetwork = databaseService.isSlowNetwork

    fun setOfflineMode(enabled: Boolean) {
        databaseService.isOfflineMode.value = enabled
    }

    fun setSlowNetwork(enabled: Boolean) {
        databaseService.isSlowNetwork.value = enabled
    }

    fun uploadTestSOS() {
        viewModelScope.launch {
            try {
                databaseService.uploadTestSOS()
                _uiEvents.emit(UiEvent.ShowToast("Test SOS Uploaded"))
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowToast("Upload Failed: ${e.message}"))
            }
        }
    }

    fun downloadTestData() {
        viewModelScope.launch {
            try {
                databaseService.downloadTestData()
                _uiEvents.emit(UiEvent.ShowToast("Test Data Downloaded"))
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowToast("Download Failed: ${e.message}"))
            }
        }
    }

    fun deleteTestRecords() {
        viewModelScope.launch {
            try {
                databaseService.deleteTestRecords()
                _uiEvents.emit(UiEvent.ShowToast("Test Records Deleted"))
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowToast("Delete Failed: ${e.message}"))
            }
        }
    }
"""

content = content.replace("    fun setCustomLocation(lat: Double, lng: Double) {", new_methods + "\n    fun setCustomLocation(lat: Double, lng: Double) {")

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
