package com.cmcorpusg.chetoauthenticator.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinSecurity {
    private const val PREFIX = "pbkdf2-sha256"
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256

    fun encode(pin: String): String {
        require(pin.matches(Regex("\\d{6}"))) { "PIN must contain exactly 6 digits" }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val hash = derive(pin, salt, ITERATIONS)
        return "$PREFIX\\$${ITERATIONS}\\$${salt.toHex()}\\$${hash.toHex()}"
    }

    fun verify(pin: String, stored: String): Boolean {
        if (!pin.matches(Regex("\\d{6}"))) return false

        // Migration path for the first native build, which stored the six digits
        // inside the encrypted vault. A successful login re-writes it as PBKDF2.
        if (!isEncoded(stored)) {
            return MessageDigest.isEqual(pin.encodeToByteArray(), stored.encodeToByteArray())
        }

        return runCatching {
            val parts = stored.split('$')
            require(parts.size == 4 && parts[0] == PREFIX)
            val iterations = parts[1].toInt()
            require(iterations in 100_000..1_000_000)
            val salt = parts[2].hexToBytes()
            val expected = parts[3].hexToBytes()
            val actual = derive(pin, salt, iterations)
            MessageDigest.isEqual(actual, expected)
        }.getOrDefault(false)
    }

    fun isEncoded(value: String): Boolean = value.startsWith("$PREFIX$")

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0)
        return ByteArray(length / 2) { index ->
            substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }
}
