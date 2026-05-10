package com.app.nepallivetv.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String
)

@Serializable
data class RegisterResponse(
    val message: String
)

@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String,
    val user_name: String,
    val email: String,
    val phone: String
) {
    val accessToken: String get() = access_token
    val userName: String get() = user_name
}
