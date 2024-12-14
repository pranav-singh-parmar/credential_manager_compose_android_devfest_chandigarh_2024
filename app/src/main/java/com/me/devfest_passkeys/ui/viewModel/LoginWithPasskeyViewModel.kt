package com.me.devfest_passkeys.ui.viewModel

import android.app.Activity
import android.util.Log
import androidx.credentials.*
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.nstant.`in`.cbor.CborDecoder
import co.nstant.`in`.cbor.model.ByteString
import co.nstant.`in`.cbor.model.NegativeInteger
import co.nstant.`in`.cbor.model.UnicodeString
import com.dashlane.dashlanepasskeydemo.model.CreatePasskeyRequest
import com.dashlane.dashlanepasskeydemo.model.CreatePasskeyResponseData
import com.dashlane.dashlanepasskeydemo.model.GetPasskeyRequest
import com.dashlane.dashlanepasskeydemo.model.GetPasskeyResponseData
import com.google.android.gms.common.GoogleApiAvailability
import com.google.gson.Gson
import com.me.devfest_passkeys.b64Decode
import com.me.devfest_passkeys.b64Encode
import com.me.devfest_passkeys.models.UserData
import com.me.devfest_passkeys.ui.state.LoginEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util
import org.bouncycastle.jce.ECNamedCurveTable
import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.time.Instant
import java.util.UUID

open class LoginWithPasskeysViewModel: ViewModel() {

    private var credentialManager: CredentialManager? = null

    private val _loginEvents = MutableSharedFlow<LoginEvent>()
    val loginEvents: SharedFlow<LoginEvent> get() = _loginEvents

    private val gson = Gson()


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
                val option = GetPublicKeyCredentialOption(getLoginPasskeyRequest(emptyList()))
                val responseData = getLoginResponse(activity, option)

                // get userdetails from responseData.id

                val userData: UserData? = null

                if (userData == null) {
                    _loginEvents.emit(LoginEvent.NoCredentials)
                    return@launch
                }
                val publicKey = userData.publicKey.toJavaPublicKey()
                if (verifySignature(responseData, publicKey)) {
                    _loginEvents.emit(LoginEvent.Success(
                        email = userData.email
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

    fun createPasswordCredential(activity: Activity, email: String) {
        viewModelScope.launch {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _loginEvents.emit(LoginEvent.EmailError)
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
                val userId = UUID.randomUUID().toString()
                Log.e("LoginWithPasskeysViewModel", "Success1")
                val response = credentialManager?.createCredential(
                    activity,
                    CreatePublicKeyCredentialRequest(getCreatePasskeyRequest(userId, email)),
                )
                Log.e("LoginWithPasskeysViewModel", "Success2")
                val responseData = gson.fromJson(
                    (response as CreatePublicKeyCredentialResponse).registrationResponseJson,
                    CreatePasskeyResponseData::class.java
                )
                val attestationObject = CborDecoder.decode(responseData.response.attestationObject.b64Decode()).first()
                val authData = (attestationObject as Map<*, *>).get(UnicodeString("authData")) as ByteString
                val publicKey = parseAuthData(authData.bytes)
                val userData = UserData(responseData.id, email, publicKey.b64Encode(), Instant.now().epochSecond)

                //create user account
                // responseData.id, userData


                Log.e("LoginWithPasskeysViewModel", "Success")
                _loginEvents.emit(LoginEvent.Success(email))
            } catch (e: CreateCredentialException) {
                Log.e("LoginWithPasskeysViewModel", "error ${e.type}")
                Log.e("LoginWithPasskeysViewModel", "error ${e.message}")
                _loginEvents.emit(LoginEvent.Error(e.message ?: ""))
            }
        }
    }

    fun getCreatePasskeyRequest(userId: String, email: String): String {
        return gson.toJson(
            CreatePasskeyRequest(
                challenge = generateFidoChallenge(),
                rp = CreatePasskeyRequest.Rp(
                    name = "Devfest Passkeys",
                    id = RELYING_PARTY_ID
                ),
                user = CreatePasskeyRequest.User(
                    id = userId,
                    name = email,
                    displayName = email
                ),
                pubKeyCredParams = listOf(
                    CreatePasskeyRequest.PubKeyCredParams(
                        type = "public-key",
                        alg = -7
                    )
                ),
                timeout = 1800000,
                attestation = "none",
                excludeCredentials = emptyList(),
                authenticatorSelection = CreatePasskeyRequest.AuthenticatorSelection(
                    authenticatorAttachment = "platform",
                    requireResidentKey = false,
                    residentKey = "required",
                    userVerification = "required"
                )
            )
        )
    }

    /**
     * Generates a random challenge for the FIDO request, that should be signed by the authenticator
     */
    private fun generateFidoChallenge(): String {
        val secureRandom = SecureRandom()
        val challengeBytes = ByteArray(32)
        secureRandom.nextBytes(challengeBytes)
        return challengeBytes.b64Encode()
    }

    /**
     * Parse the authData from the attestationObject to get the public key
     */
    private fun parseAuthData(buffer: ByteArray): ByteArray {
        /*val rpIdHash = buffer.copyOfRange(0, 32)
        val flags = buffer.copyOfRange(32, 33)
        val signCount = buffer.copyOfRange(33, 37)
        val aaguid = buffer.copyOfRange(37, 53)*/
        val credentialIdLength = buffer.copyOfRange(53, 55)
        //val credentialId = buffer.copyOfRange(55, 55 + credentialIdLength[1].toInt())
        return buffer.copyOfRange(55 + credentialIdLength[1].toInt(), buffer.size)
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

    /**
     * Call the credential manager to create a passkey login request
     */
    private suspend fun getLoginResponse(
        activity: Activity,
        option: GetPublicKeyCredentialOption
    ): GetPasskeyResponseData {
        val getCredRequest = GetCredentialRequest(listOf(option))
        val response = credentialManager?.getCredential(activity, getCredRequest)
        val cred = response?.credential as PublicKeyCredential
        return gson.fromJson(cred.authenticationResponseJson, GetPasskeyResponseData::class.java)
    }

    /**
     * Convert the user's public key, stored as String, to a java PublicKey
     */
    private fun String.toJavaPublicKey(): PublicKey {
        val decoded = CborDecoder.decode(this.b64Decode()).first() as co.nstant.`in`.cbor.model.Map
        val publicKeyX = decoded[NegativeInteger(-2)] as ByteString
        val publicKeyY = decoded[NegativeInteger(-3)] as ByteString
        val ecPoint = ECPoint(BigInteger(1, publicKeyX.bytes), BigInteger(1, publicKeyY.bytes))
        val params = ECNamedCurveTable.getParameterSpec("secp256r1")
        val ellipticCurve = EC5Util.convertCurve(params.curve, params.seed)
        val params2 = EC5Util.convertSpec(ellipticCurve, params)
        val keySpec = ECPublicKeySpec(ecPoint, params2)
        return KeyFactory.getInstance("EC").generatePublic(keySpec)
    }

    /**
     * Check if the signature is valid by signing the clientDataJSON with the public key
     */
    private fun verifySignature(responseData: GetPasskeyResponseData, publicKey: PublicKey): Boolean {
        val signature = responseData.response.signature.b64Decode()
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initVerify(publicKey)
        val md = MessageDigest.getInstance("SHA-256")
        val clientDataHash = md.digest(responseData.response.clientDataJSON.b64Decode())
        val signatureBase = responseData.response.authenticatorData.b64Decode() + clientDataHash
        sig.update(signatureBase)
        return sig.verify(signature)
    }

    fun getLoginPasskeyRequest(allowedCredential: List<String>): String {
        return gson.toJson(
            GetPasskeyRequest(
                challenge = generateFidoChallenge(),
                timeout = 1800000,
                userVerification = "required",
                rpId = RELYING_PARTY_ID,
                allowCredentials = allowedCredential.map {
                    GetPasskeyRequest.AllowCredentials(
                        id = it,
                        transports = listOf(),
                        type = "public-key"
                    )
                }
            )
        )
    }

    companion object {
        private const val RELYING_PARTY_ID = "google.com"
    }
}