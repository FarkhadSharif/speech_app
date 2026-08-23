package com.farkhad.speechapp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farkhad.speechapp.data.FirebaseRepository
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var childName by mutableStateOf("")
    var childAge by mutableStateOf("")
    var parentPin by mutableStateOf("")
    var isPasswordVisible by mutableStateOf(false)
    var isSignUpMode by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isAuthenticated by mutableStateOf(repository.isUserLoggedIn)

    val currentUserUid: String?
        get() = repository.currentUserUid

    fun checkAuthStatus() {
        isAuthenticated = repository.isUserLoggedIn
    }

    fun signOut() {
        repository.signOut()
        isAuthenticated = false
        email = ""
        password = ""
        childName = ""
        childAge = ""
        parentPin = ""
        errorMessage = null
    }

    private fun validatePassword(pass: String): String? {
        if (pass.length < 8) {
            return "Құпия сөз кемінде 8 таңбадан тұруы керек."
        }
        if (!pass.any { it.isUpperCase() }) {
            return "Құпия сөзде кемінде бір бас әріп (A-Z) болуы керек."
        }
        if (!pass.any { it.isLowerCase() }) {
            return "Құпия сөзде кемінде бір кіші әріп (a-z) болуы керек."
        }
        if (!pass.any { it.isDigit() }) {
            return "Құпия сөзде кемінде бір сан (0-9) болуы керек."
        }
        return null
    }

    fun authenticate() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Барлық өрістерді толтырыңыз."
            return
        }

        if (isSignUpMode) {
            if (childName.isBlank() || childAge.isBlank() || parentPin.length != 4) {
                errorMessage = "Барлық өрістерді, соның ішінде 4 таңбалы PIN-кодты толтырыңыз."
                return
            }
            val passValidationMsg = validatePassword(password)
            if (passValidationMsg != null) {
                errorMessage = passValidationMsg
                return
            }
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val result = if (isSignUpMode) {
                repository.signUp(email.trim(), password.trim(), childName.trim(), childAge.trim(), parentPin.trim())
            } else {
                repository.signIn(email.trim(), password.trim())
            }

            isLoading = false
            result.onSuccess {
                isAuthenticated = true
            }.onFailure { error ->
                val msg = error.localizedMessage
                // Show custom Kazakh warnings from repository, otherwise fallback
                errorMessage = if (msg != null && (
                        msg.contains("Бұл") || 
                        msg.contains("Тіркелу") || 
                        msg.contains("Кіру") || 
                        msg.contains("қате")
                    )) {
                    msg
                } else {
                    "Қайтадан байқап көріңіз, күтпеген қате орын алды."
                }
            }
        }
    }
}