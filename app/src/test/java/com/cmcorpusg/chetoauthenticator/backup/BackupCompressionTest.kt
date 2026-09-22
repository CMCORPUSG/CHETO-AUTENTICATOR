package com.cmcorpusg.chetoauthenticator.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCompressionTest {

    @Test
    fun gzipRoundTripPreservesExactJsonAndReducesRepetitivePayload() {
        val json = buildString {
            append("{\"accounts\":[")
            repeat(500) { index ->
                if (index > 0) append(',')
                append(
                    "{\"issuer\":\"Example Service\",\"label\":\"user$index@example.com\"," +
                        "\"category\":\"Trabajo\",\"notes\":\"repetitive backup data\"}"
                )
            }
            append("]}")
        }

        val compressed = BackupCrypto.compressUtf8(json)
        val restored = BackupCrypto.decompressUtf8(compressed)

        assertEquals(json, restored)
        assertTrue(compressed.size < json.toByteArray(Charsets.UTF_8).size)
    }
}
