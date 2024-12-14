package com.me.devfest_passkeys.ui.viewModel

import android.app.Activity
import androidx.credentials.*
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.GoogleApiAvailability
import com.me.devfest_passkeys.ui.state.LoginEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

open class LoginViewModel: ViewModel() {

    private var credentialManager: CredentialManager? = null

    private val _loginEvents = MutableSharedFlow<LoginEvent>()
    val loginEvents: SharedFlow<LoginEvent> get() = _loginEvents


    fun retrieveCredentials(activity: Activity) {
        viewModelScope.launch {
            if (!isCredentialManagerAvailable(activity)) {
                // Notify that CredentialManager is not supported
                _loginEvents.emit(LoginEvent.Error("CredentialManager is not supported on this device."))
                return@launch
            }

            if (!isGooglePlayServicesAvailable(activity)) {
                // Notify that Google Play Services is not enabled
                _loginEvents.emit(LoginEvent.Error("Google Play Services is not enabled on this device."))
                return@launch
            }

            try {
                val response = credentialManager?.getCredential(
                    context = activity,
                    GetCredentialRequest(credentialOptions = listOf(GetPasswordOption()))
                )

                val credential = response?.credential

                if (credential is PasswordCredential) {
                    // credential.id
                    _loginEvents.emit(LoginEvent.Success(
                        email = credential.id
                    ))
                } else {
                    _loginEvents.emit(LoginEvent.NoCredentials)
                }
            } catch (e: GetCredentialException) {
                if (e.type == android.credentials.GetCredentialException.TYPE_NO_CREDENTIAL) {
                    _loginEvents.emit(LoginEvent.NoCredentials)
                } else {
                    _loginEvents.emit(LoginEvent.Error(e.message ?: "Unknown error"))
                }
            }
        }
    }

    fun createPasswordCredential(activity: Activity, email: String, password: String) {
        viewModelScope.launch {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _loginEvents.emit(LoginEvent.EmailError)
                return@launch
            }
            if (password.count() < 6) {
                _loginEvents.emit(LoginEvent.PasswordError)
                return@launch
            }
            if (!isCredentialManagerAvailable(activity)) {
                // Notify that CredentialManager is not supported
                _loginEvents.emit(LoginEvent.Error("CredentialManager is not supported on this device."))
                return@launch
            }

            if (!isGooglePlayServicesAvailable(activity)) {
                // Notify that Google Play Services is not enabled
                _loginEvents.emit(LoginEvent.Error("Google Play Services is not enabled on this device."))
                return@launch
            }

            //account creation process here

            try {
                credentialManager?.createCredential(
                    request = CreatePasswordRequest(
                        email,
                        password
                    ), // CreatePasswordRequest to specify the new credentials
                    context = activity
                )
            } catch (e: CreateCredentialException) {
                // _loginEvents.emit(LoginEvent.Error(e.message ?: "Error creating credential"))
            }
            _loginEvents.emit(LoginEvent.Success(email))
        }
    }

    /**
     * Check if CredentialManager is available on the device.
     *
     * @param activity Activity.
     * @return True if CredentialManager is available, false otherwise.
     */
    private fun isCredentialManagerAvailable(activity: Activity): Boolean {
        if (credentialManager == null) {
            try {
                credentialManager = CredentialManager.create(activity)
                return true
            } catch (e: Exception) {
                return false
            }
        }
        return true
    }


    /**
     * Function to check if Google Play Services is enabled
     *
     * @param activity Activity.
     * @return True if is enabled, false otherwise.
     */
    private fun isGooglePlayServicesAvailable(activity: Activity): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(activity)
        return status == com.google.android.gms.common.ConnectionResult.SUCCESS
    }
}
