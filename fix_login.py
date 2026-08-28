import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

replacement = """    fun loginUser(email: String, pass: String) {
        viewModelScope.launch {
            authService.login(email, pass)
        }
    }
    fun registerUser(name: String, email: String, phone: String, medical: String, contactName: String, contactPhone: String, pass: String) {
        viewModelScope.launch {
            val user = User(
                name = name,
                email = email,
                phone = phone,
                medicalInfo = medical,
                emergencyContactName = contactName,
                emergencyContactPhone = contactPhone
            )
            authService.register(user, pass)
        }
    }
    fun resetPassword(email: String) {
        viewModelScope.launch {
            authService.resetPassword(email)
        }
    }
    fun logout() {
        authService.logout()
    }
    fun updateUserProfile(updatedUser: User) {
        viewModelScope.launch {
            authService.updateProfile(updatedUser)
        }
    }"""

old_block = """    fun loginUser(email: String, pass: String) { }
    fun registerUser(name: String, email: String, phone: String, medical: String, contactName: String, contactPhone: String, pass: String) { }
    fun resetPassword(email: String) { }
    fun logout() { }
    fun updateUserProfile(updatedUser: User) { }"""

content = content.replace(old_block, replacement)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
