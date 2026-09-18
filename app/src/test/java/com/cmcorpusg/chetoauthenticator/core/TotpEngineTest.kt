package com.cmcorpusg.chetoauthenticator.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TotpEngineTest {
    @Test
    fun rfc6238_sha1_vector_at_59_seconds() {
        val secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

        val code = TotpEngine.generate(
            secretBase32 = secret,
            epochSeconds = 59,
            digits = 8,
            period = 30,
            algorithm = "SHA1"
        )

        assertEquals("94287082", code)
    }

    @Test
    fun rfc6238_sha1_vector_at_1111111109_seconds() {
        val secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

        val code = TotpEngine.generate(
            secretBase32 = secret,
            epochSeconds = 1_111_111_109,
            digits = 8,
            period = 30,
            algorithm = "SHA1"
        )

        assertEquals("07081804", code)
    }
}
