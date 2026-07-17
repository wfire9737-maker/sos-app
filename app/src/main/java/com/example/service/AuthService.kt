package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.User
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthService(private val context: Context) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    // Fallback SharedPreferences for Demo Mode
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("guardian_sos_auth", Context.MODE_PRIVATE)
    
    val isDemoMode: Boolean
        get() = firebaseAuth == null

    init {
        checkFirebaseAvailability()
    }

    fun checkFirebaseAvailability() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            firebaseAuth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            Log.d("AuthService", "Firebase Auth & Firestore successfully initialized!")
            
            // Check for existing Firebase user session
            firebaseAuth?.currentUser?.let { firebaseUser ->
                loadUserProfile(firebaseUser.uid)
            } ?: run {
                _authState.value = AuthState.Initial
            }
        } catch (e: Exception) {
            firebaseAuth = null
            firestore = null
            Log.w("AuthService", "Firebase App not fully configured (missing google-services.json). Falling back to Offline Demo Mode: ${e.message}")
            
            // Check for existing Demo user session
            val activeUserJson = sharedPrefs.getString("current_user", null)
            if (activeUserJson != null) {
                try {
                    val userObj = JSONObject(activeUserJson)
                    val demoUser = userFromJsonObject(userObj)
                    _authState.value = AuthState.Success(demoUser)
                } catch (e: Exception) {
                    _authState.value = AuthState.Initial
                }
            } else {
                _authState.value = AuthState.Initial
            }
        }
    }

    private fun loadUserProfile(uid: String) {
        val fs = firestore
        if (fs != null) {
            _authState.value = AuthState.Loading
            fs.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val profile = User.fromMap(document.data ?: emptyMap())
                        _authState.value = AuthState.Success(profile)
                    } else {
                        // Profile missing but auth exists, create default profile
                        val defaultUser = User(uid = uid, email = firebaseAuth?.currentUser?.email ?: "", name = "Guardian User")
                        fs.collection("users").document(uid).set(defaultUser.toMap())
                        _authState.value = AuthState.Success(defaultUser)
                    }
                }
                .addOnFailureListener { exception ->
                    _authState.value = AuthState.Error(exception.localizedMessage ?: "Failed to load profile")
                }
        }
    }

    suspend fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty.")
            return
        }

        _authState.value = AuthState.Loading

        val auth = firebaseAuth
        if (auth != null) {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                result.user?.let { firebaseUser ->
                    val fs = firestore
                    if (fs != null) {
                        val doc = fs.collection("users").document(firebaseUser.uid).get().await()
                        val user = if (doc.exists()) {
                            User.fromMap(doc.data ?: emptyMap())
                        } else {
                            User(uid = firebaseUser.uid, email = email, name = "Guardian User")
                        }
                        _authState.value = AuthState.Success(user)
                    } else {
                        val simpleUser = User(uid = firebaseUser.uid, email = email)
                        _authState.value = AuthState.Success(simpleUser)
                    }
                } ?: run {
                    _authState.value = AuthState.Error("Failed to obtain signed in user.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Authentication failed.")
            }
        } else {
            // High-Fidelity Local Demo Authentication
            val userJson = sharedPrefs.getString("user_reg_$email", null)
            if (userJson != null) {
                try {
                    val userObj = JSONObject(userJson)
                    val storedPassword = userObj.optString("password")
                    if (storedPassword == password) {
                        val loggedInUser = userFromJsonObject(userObj)
                        persistDemoSession(loggedInUser)
                        _authState.value = AuthState.Success(loggedInUser)
                    } else {
                        _authState.value = AuthState.Error("Incorrect password for this account.")
                    }
                } catch (e: Exception) {
                    _authState.value = AuthState.Error("Error processing demo credentials.")
                }
            } else {
                if (email == "demo@guardian.sos" && password == "password123") {
                    val demoUser = User(
                        uid = "demo-uid-123",
                        name = "Safety Officer Demo",
                        email = "demo@guardian.sos",
                        phone = "+1-555-0199",
                        medicalInfo = "Allergic to Penicillin",
                        emergencyContactName = "Dispatch HQ",
                        emergencyContactPhone = "911",
                        role = "System Administrator",
                        bloodType = "A+",
                        allergies = "Penicillin, Peanuts",
                        conditions = "Asthma",
                        medications = "Albuterol Inhaler"
                    )
                    preRegisterDemoUser(demoUser, "password123")
                    persistDemoSession(demoUser)
                    _authState.value = AuthState.Success(demoUser)
                } else {
                    _authState.value = AuthState.Error("No account found for $email. Tip: Use demo@guardian.sos with password123, or tap register.")
                }
            }
        }
    }

    suspend fun register(
        user: User, 
        password: String
    ) {
        if (user.email.isBlank() || password.isBlank() || user.name.isBlank()) {
            _authState.value = AuthState.Error("Name, Email, and Password cannot be empty.")
            return
        }

        _authState.value = AuthState.Loading

        val auth = firebaseAuth
        if (auth != null) {
            try {
                val result = auth.createUserWithEmailAndPassword(user.email, password).await()
                result.user?.let { firebaseUser ->
                    val finalUser = user.copy(uid = firebaseUser.uid)
                    firestore?.collection("users")?.document(firebaseUser.uid)?.set(finalUser.toMap())?.await()
                    _authState.value = AuthState.Success(finalUser)
                } ?: run {
                    _authState.value = AuthState.Error("Registration failed.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Registration failed.")
            }
        } else {
            // Local Demo Registration
            val existing = sharedPrefs.getString("user_reg_${user.email}", null)
            if (existing != null) {
                _authState.value = AuthState.Error("An account with this email already exists.")
                return
            }

            val uid = "demo-" + java.util.UUID.randomUUID().toString().take(8)
            val finalUser = user.copy(uid = uid)
            val userObj = userToJsonObject(finalUser, password)

            sharedPrefs.edit()
                .putString("user_reg_${finalUser.email}", userObj.toString())
                .apply()

            persistDemoSession(finalUser)
            _authState.value = AuthState.Success(finalUser)
        }
    }

    suspend fun updateProfile(updatedUser: User) {
        _authState.value = AuthState.Loading
        val fs = firestore
        if (fs != null) {
            try {
                fs.collection("users").document(updatedUser.uid).set(updatedUser.toMap()).await()
                _authState.value = AuthState.Success(updatedUser)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to update profile in Firestore")
            }
        } else {
            // Local Demo Save
            persistDemoSession(updatedUser)
            
            // Load old password if existing to preserve registration integrity
            val oldUserJson = sharedPrefs.getString("user_reg_${updatedUser.email}", null)
            val password = if (oldUserJson != null) {
                JSONObject(oldUserJson).optString("password", "password123")
            } else {
                "password123"
            }
            
            val userObj = userToJsonObject(updatedUser, password)
            sharedPrefs.edit()
                .putString("user_reg_${updatedUser.email}", userObj.toString())
                .apply()

            _authState.value = AuthState.Success(updatedUser)
        }
    }

    suspend fun resetPassword(email: String): Boolean {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Email address is required.")
            return false
        }

        val auth = firebaseAuth
        if (auth != null) {
            return try {
                auth.sendPasswordResetEmail(email).await()
                true
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to send reset email.")
                false
            }
        } else {
            // Demo mode forgot password check
            val userJson = sharedPrefs.getString("user_reg_$email", null)
            return if (userJson != null || email == "demo@guardian.sos") {
                true // Simulated successful password reset email
            } else {
                _authState.value = AuthState.Error("Email not found in local demo registers.")
                false
            }
        }
    }

    fun logout() {
        firebaseAuth?.signOut()
        sharedPrefs.edit().remove("current_user").apply()
        _authState.value = AuthState.Initial
    }

    private fun persistDemoSession(user: User) {
        val userObj = userToJsonObject(user)
        sharedPrefs.edit()
            .putString("current_user", userObj.toString())
            .apply()
    }

    private fun preRegisterDemoUser(user: User, pass: String) {
        val userObj = userToJsonObject(user, pass)
        sharedPrefs.edit()
            .putString("user_reg_${user.email}", userObj.toString())
            .apply()
    }

    // --- JSON PARSING HELPERS ---
    private fun userFromJsonObject(userObj: JSONObject): User {
        return User(
            uid = userObj.optString("uid"),
            name = userObj.optString("name"),
            email = userObj.optString("email"),
            phone = userObj.optString("phone"),
            medicalInfo = userObj.optString("medicalInfo"),
            emergencyContactName = userObj.optString("emergencyContactName"),
            emergencyContactPhone = userObj.optString("emergencyContactPhone"),
            role = userObj.optString("role", "User"),
            createdAt = userObj.optLong("createdAt", System.currentTimeMillis()),
            photoUri = if (userObj.has("photoUri") && !userObj.isNull("photoUri")) userObj.getString("photoUri") else null,
            bloodType = userObj.optString("bloodType", ""),
            allergies = userObj.optString("allergies", ""),
            conditions = userObj.optString("conditions", ""),
            medications = userObj.optString("medications", "")
        )
    }

    private fun userToJsonObject(user: User, password: String? = null): JSONObject {
        val userObj = JSONObject()
        userObj.put("uid", user.uid)
        userObj.put("name", user.name)
        userObj.put("email", user.email)
        userObj.put("phone", user.phone)
        userObj.put("medicalInfo", user.medicalInfo)
        userObj.put("emergencyContactName", user.emergencyContactName)
        userObj.put("emergencyContactPhone", user.emergencyContactPhone)
        userObj.put("role", user.role)
        userObj.put("createdAt", user.createdAt)
        userObj.put("photoUri", user.photoUri ?: JSONObject.NULL)
        userObj.put("bloodType", user.bloodType)
        userObj.put("allergies", user.allergies)
        userObj.put("conditions", user.conditions)
        userObj.put("medications", user.medications)
        if (password != null) {
            userObj.put("password", password)
        }
        return userObj
    }
}
