package com.me.devfest_passkeys.ui.state

sealed class LoginEvent {
    data object Idle : LoginEvent()
    data object EmailError : LoginEvent()
    data object PasswordError : LoginEvent()
    data class Success(val email: String) : LoginEvent()
    data object NoCredentials : LoginEvent()
    data class Error(val message: String) : LoginEvent()
}