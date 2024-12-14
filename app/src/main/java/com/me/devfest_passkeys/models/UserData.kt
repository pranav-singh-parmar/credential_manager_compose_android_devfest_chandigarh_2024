package com.me.devfest_passkeys.models

data class UserData(
    val credentialId: String,
    val email: String,
    val publicKey: String,
    val creationDate: Long
)