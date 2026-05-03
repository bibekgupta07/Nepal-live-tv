package com.app.nepallivetv.data.model

import kotlinx.serialization.SerialName
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
data class LoginRequest(
    @SerialName("login_id") val loginId: String,
    val password: String
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("user_name") val userName: String,
    val email: String,
    val phone: String
)