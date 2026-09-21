package com.cmcorpusg.chetoauthenticator.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

class GoogleAuthMigrationParserTest {
    @Test
    fun parsesTotpMigrationPayload() {
        val otp = message(
            fieldBytes(1, byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)),
            fieldString(2, "GitHub:me@example.com"),
            fieldString(3, "GitHub"),
            fieldVarint(4, 1),
            fieldVarint(5, 1),
            fieldVarint(6, 2)
        )
        val payload = message(
            fieldBytes(1, otp),
            fieldVarint(2, 1),
            fieldVarint(3, 1),
            fieldVarint(4, 0),
            fieldVarint(5, 42)
        )
        val encoded = Base64.getEncoder().encodeToString(payload)
        val uri = "otpauth-migration://offline?data=" +
            URLEncoder.encode(encoded, StandardCharsets.UTF_8.name())

        val result = GoogleAuthMigrationParser.parse(uri)

        assertEquals(1, result.entries.size)
        assertEquals("GitHub", result.entries.single().issuer)
        assertEquals("me@example.com", result.entries.single().label)
        assertEquals(6, result.entries.single().digits)
        assertEquals("SHA1", result.entries.single().algorithm)
        assertEquals(1, result.batchSize)
        assertEquals(42L, result.batchId)
    }

    private fun message(vararg fields: ByteArray): ByteArray =
        fields.fold(ByteArray(0)) { acc, field -> acc + field }

    private fun fieldString(number: Int, value: String): ByteArray =
        fieldBytes(number, value.toByteArray(StandardCharsets.UTF_8))

    private fun fieldBytes(number: Int, value: ByteArray): ByteArray =
        varint(((number shl 3) or 2).toLong()) + varint(value.size.toLong()) + value

    private fun fieldVarint(number: Int, value: Long): ByteArray =
        varint((number shl 3).toLong()) + varint(value)

    private fun varint(value: Long): ByteArray {
        var current = value
        val bytes = ArrayList<Byte>()
        while (true) {
            if ((current and -128L) == 0L) {
                bytes += current.toByte()
                return bytes.toByteArray()
            }
            bytes += ((current and 0x7F) or 0x80).toByte()
            current = current ushr 7
        }
    }
}
