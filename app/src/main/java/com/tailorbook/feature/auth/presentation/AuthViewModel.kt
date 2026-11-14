package com.tailorbook.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailorbook.feature.auth.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoginSuccessful: Boolean = false,
    val isRegistrationSuccessful: Boolean = false,
    val isPasswordResetRequested: Boolean = false,
    val isPasswordResetCompleted: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signInWithEmail(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (!isValidEmail(trimmedEmail)) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = authRepository.signInWithEmail(trimmedEmail, password)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isLoginSuccessful = true) }
                },
                onFailure = { error ->
                    val msg = if (error.message?.contains("email_not_confirmed", ignoreCase = true) == true) {
                        "Please confirm your email address via the link or code sent to your inbox before signing in."
                    } else {
                        error.message ?: "Login failed. Please check your credentials."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
            )
        }
    }

    fun signUpWithEmail(
        email: String,
        password: String,
        confirmPass: String,
        shopName: String,
        ownerName: String,
        phone: String?,
        upiId: String? = null
    ) {
        val trimmedEmail = email.trim()
        if (shopName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your boutique or shop name.") }
            return
        }
        if (ownerName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter the proprietor / owner name.") }
            return
        }
        if (!isValidEmail(trimmedEmail)) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters long.") }
            return
        }
        if (password != confirmPass) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = authRepository.signUpWithEmail(trimmedEmail, password, shopName.trim(), ownerName.trim(), phone?.trim(), upiId?.trim())
            result.fold(
                onSuccess = { session ->
                    val successNote = if (session.accessToken.isBlank()) {
                        "Account created! Please check your email for the confirmation link to complete registration."
                    } else {
                        "Account registered successfully!"
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRegistrationSuccessful = true,
                            successMessage = successNote
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Registration failed.") }
                }
            )
        }
    }

    fun requestPasswordResetOtp(email: String) {
        val trimmedEmail = email.trim()
        if (!isValidEmail(trimmedEmail)) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = authRepository.requestPasswordResetOtp(trimmedEmail)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPasswordResetRequested = true,
                            successMessage = "6-digit reset code sent to your email."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Failed to send reset code.") }
                }
            )
        }
    }

    fun resetPasswordWithOtp(email: String, otp: String, newPassword: String) {
        val trimmedEmail = email.trim()
        val trimmedOtp = otp.trim()
        if (trimmedOtp.length != 6 || !trimmedOtp.all { it.isDigit() }) {
            _uiState.update { it.copy(errorMessage = "Please enter the valid 6-digit verification code.") }
            return
        }
        if (newPassword.length < 6) {
            _uiState.update { it.copy(errorMessage = "New password must be at least 6 characters.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = authRepository.resetPasswordWithOtp(trimmedEmail, trimmedOtp, newPassword)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPasswordResetCompleted = true,
                            successMessage = "Password reset successfully! You may now sign in."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Failed to reset password.") }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { AuthUiState() }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
