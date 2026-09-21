package com.cmcorpusg.chetoauthenticator.core

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64

data class MigrationOtp(
    val secret: String,
    val label: String,
    val issuer: String,
    val digits: Int,
    val algorithm: String
)

data class MigrationPayload(
    val entries: List<MigrationOtp>,
    val batchSize: Int,
    val batchIndex: Int,
    val batchId: Long,
    val skippedUnsupported: Int
)

object GoogleAuthMigrationParser {
    fun parse(uri: String): MigrationPayload {
        require(uri.startsWith("otpauth-migration://offline?")) { "Formato de migración inválido" }
        val rawData = uri.substringAfter("data=", "").substringBefore('&')
        require(rawData.isNotBlank()) { "Falta data" }

        val decoded = URLDecoder.decode(rawData, StandardCharsets.UTF_8.name()).replace(' ', '+')
        val padded = decoded + "=".repeat((4 - decoded.length % 4) % 4)
        val payload = Base64.getDecoder().decode(padded)
        val reader = Reader(payload)

        val entries = mutableListOf<MigrationOtp>()
        var batchSize = 1
        var batchIndex = 0
        var batchId = 0L
        var skipped = 0

        while (!reader.eof()) {
            val tag = reader.varint().toInt()
            val field = tag ushr 3
            val wire = tag and 7
            when {
                field == 1 && wire == 2 -> {
                    val parsed = parseOtp(reader.bytes())
                    if (parsed != null) entries += parsed else skipped++
                }
                field == 3 && wire == 0 -> batchSize = reader.varint().toInt().coerceAtLeast(1)
                field == 4 && wire == 0 -> batchIndex = reader.varint().toInt().coerceAtLeast(0)
                field == 5 && wire == 0 -> batchId = reader.varint()
                else -> reader.skip(wire)
            }
        }

        require(entries.isNotEmpty() || skipped > 0) { "El QR no contiene cuentas" }
        return MigrationPayload(entries, batchSize, batchIndex, batchId, skipped)
    }

    private fun parseOtp(bytes: ByteArray): MigrationOtp? {
        val reader = Reader(bytes)
        var secret = ByteArray(0)
        var name = ""
        var issuer = ""
        var algorithm = 1
        var digits = 1
        var type = 0

        while (!reader.eof()) {
            val tag = reader.varint().toInt()
            val field = tag ushr 3
            val wire = tag and 7
            when {
                field == 1 && wire == 2 -> secret = reader.bytes()
                field == 2 && wire == 2 -> name = reader.bytes().toString(StandardCharsets.UTF_8)
                field == 3 && wire == 2 -> issuer = reader.bytes().toString(StandardCharsets.UTF_8)
                field == 4 && wire == 0 -> algorithm = reader.varint().toInt()
                field == 5 && wire == 0 -> digits = reader.varint().toInt()
                field == 6 && wire == 0 -> type = reader.varint().toInt()
                else -> reader.skip(wire)
            }
        }

        if (secret.isEmpty()) return null
        if (type != 2) return null

        val algorithmName = when (algorithm) {
            0, 1 -> "SHA1"
            2 -> "SHA256"
            3 -> "SHA512"
            else -> return null
        }
        val digitCount = when (digits) {
            0, 1 -> 6
            2 -> 8
            else -> return null
        }

        val effectiveIssuer = issuer.ifBlank {
            name.substringBefore(':', "").trim()
        }.ifBlank { "Servicio" }
        val effectiveLabel = if (name.contains(':')) name.substringAfter(':').trim() else name.trim()

        return MigrationOtp(
            secret = base32(secret),
            label = effectiveLabel.ifBlank { "Cuenta" },
            issuer = effectiveIssuer,
            digits = digitCount,
            algorithm = algorithmName
        )
    }

    private fun base32(input: ByteArray): String {
        if (input.isEmpty()) return ""
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val out = StringBuilder((input.size * 8 + 4) / 5)
        var buffer = 0
        var bitsLeft = 0
        input.forEach { byte ->
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val index = (buffer shr (bitsLeft - 5)) and 31
                out.append(alphabet[index])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            out.append(alphabet[(buffer shl (5 - bitsLeft)) and 31])
        }
        return out.toString()
    }

    private class Reader(private val data: ByteArray) {
        private var position = 0

        fun eof(): Boolean = position >= data.size

        fun varint(): Long {
            var result = 0L
            var shift = 0
            while (shift < 64) {
                require(position < data.size) { "Protobuf truncado" }
                val value = data[position++].toInt() and 0xFF
                result = result or ((value and 0x7F).toLong() shl shift)
                if ((value and 0x80) == 0) return result
                shift += 7
            }
            error("Varint inválido")
        }

        fun bytes(): ByteArray {
            val length = varint().toInt()
            require(length >= 0 && position + length <= data.size) { "Longitud protobuf inválida" }
            return data.copyOfRange(position, position + length).also { position += length }
        }

        fun skip(wire: Int) {
            when (wire) {
                0 -> varint()
                1 -> advance(8)
                2 -> advance(varint().toInt())
                5 -> advance(4)
                else -> error("Wire type protobuf no soportado: $wire")
            }
        }

        private fun advance(count: Int) {
            require(count >= 0 && position + count <= data.size) { "Protobuf truncado" }
            position += count
        }
    }
}
