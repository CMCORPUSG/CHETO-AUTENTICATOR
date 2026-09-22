package com.cmcorpusg.chetoauthenticator.data

import java.util.UUID

data class AuthAccount(
    val id: String = UUID.randomUUID().toString(),
    val issuer: String,
    val label: String,
    val secret: String,
    val digits: Int = 6,
    val period: Int = 30,
    val algorithm: String = "SHA1"
)
