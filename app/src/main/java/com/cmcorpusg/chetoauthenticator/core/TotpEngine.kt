package com.cmcorpusg.chetoauthenticator.core

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object TotpEngine {
    fun generate(
        secretBase32: String,
        epochSeconds: Long = System.currentTimeMillis() / 1000,
        digits: Int = 6,
        period: Int = 30,
        algorithm: String = "SHA1"
    ): String {
        require(digits in 6..8) { "digits must be between 6 and 8" }
        require(period > 0) { "period must be positive" }

        val key = decodeBase32(secretBase32)
        val counter = epochSeconds / period
        val data = ByteBuffer.allocate(8).putLong(counter).array()
        val macAlgorithm = when (algorithm.uppercase()) {
            "SHA1" -> "HmacSHA1"
            "SHA256" -> "HmacSHA256"
            "SHA512" -> "HmacSHA512"
            else -> error("Unsupported algorithm: $algorithm")
        }

        val mac = Mac.getInstance(macAlgorithm)
        mac.init(SecretKeySpec(key, macAlgorithm))
        val hash = mac.doFinal(data)

        val offset = hash.last().toInt() and 0x0f
        val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
            ((hash[offset + 1].toInt() and 0xff) shl 16) or
            ((hash[offset + 2].toInt() and 0xff) shl 8) or
            (hash[offset + 3].toInt() and 0xff)

        val modulus = 10.0.pow(digits).toInt()
        return (binary % modulus).toString().padStart(digits, '0')
    }

    fun secondsRemaining(epochSeconds: Long, period: Int): Int {
        val elapsed = (epochSeconds % period).toInt()
        return if (elapsed == 0) period else period - elapsed
    }

    internal fun decodeBase32(value: String): ByteArray {
        val clean = value
            .uppercase()
            .filter { !it.isWhitespace() && it != '-' && it != '=' }

        require(clean.isNotBlank()) { "Secret is empty" }

        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        var buffer = 0
        var bitsLeft = 0
        val output = ArrayList<Byte>()

        for (char in clean) {
            val index = alphabet.indexOf(char)
            require(index >= 0) { "Invalid Base32 character: $char" }

            buffer = (buffer shl 5) or index
            bitsLeft += 5

            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output += ((buffer shr bitsLeft) and 0xff).toByte()
            }
        }

        return output.toByteArray()
    }
}
