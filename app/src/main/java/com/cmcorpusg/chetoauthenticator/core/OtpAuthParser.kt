package com.cmcorpusg.chetoauthenticator.core

import android.net.Uri
import com.cmcorpusg.chetoauthenticator.data.AuthAccount
import java.util.UUID

object OtpAuthParser {
    fun parse(value: String): AuthAccount {
        val uri = Uri.parse(value.trim())
        require(uri.scheme.equals("otpauth", ignoreCase = true)) { "QR is not an otpauth URI" }
        require(uri.host.equals("totp", ignoreCase = true)) { "Only TOTP accounts are supported" }

        val rawLabel = uri.path?.removePrefix("/")?.trim().orEmpty()
        require(rawLabel.isNotBlank()) { "Account label is missing" }

        val secret = uri.getQueryParameter("secret")?.trim().orEmpty()
        require(secret.isNotBlank()) { "TOTP secret is missing" }

        val issuerFromQuery = uri.getQueryParameter("issuer")?.trim().orEmpty()
        val issuerFromLabel = rawLabel.substringBefore(":", "").trim()
        val accountLabel = rawLabel.substringAfter(":", rawLabel).trim()
        val issuer = issuerFromQuery.ifBlank { issuerFromLabel.ifBlank { "Cuenta" } }

        val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6
        val period = uri.getQueryParameter("period")?.toIntOrNull() ?: 30
        val algorithm = uri.getQueryParameter("algorithm")?.uppercase() ?: "SHA1"

        require(digits in 6..8) { "Unsupported number of digits" }
        require(period in 1..300) { "Unsupported TOTP period" }
        require(algorithm in setOf("SHA1", "SHA256", "SHA512")) { "Unsupported TOTP algorithm" }

        return AuthAccount(
            id = UUID.randomUUID().toString(),
            issuer = issuer,
            label = accountLabel,
            secret = secret,
            digits = digits,
            period = period,
            algorithm = algorithm
        )
    }
}
