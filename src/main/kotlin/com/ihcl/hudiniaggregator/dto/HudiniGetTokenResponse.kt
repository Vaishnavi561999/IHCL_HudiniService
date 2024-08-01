package com.ihcl.hudiniaggregator.dto

data class HudiniGetTokenResponse(
    val data: GetTokenData
)
data class GetTokenData(
    val auth: Auth
)
data class Auth(
    val getToken: GetToken
)
data class GetToken(
    val accessToken: String,
    val expiresIn: Int,
    val tokenType: String
)