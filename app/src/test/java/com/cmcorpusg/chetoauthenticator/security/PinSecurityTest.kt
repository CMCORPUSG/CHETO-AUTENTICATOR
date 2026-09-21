package com.cmcorpusg.chetoauthenticator.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSecurityTest {
    @Test
    fun encodedPinDoesNotContainPlainPinAndVerifies() {
        val encoded = PinSecurity.encode("123456")

        assertNotEquals("123456", encoded)
        assertFalse(encoded.contains("123456"))
        assertTrue(PinSecurity.isEncoded(encoded))
        assertTrue(PinSecurity.verify("123456", encoded))
        assertFalse(PinSecurity.verify("654321", encoded))
    }

    @Test
    fun legacySixDigitPinCanBeVerifiedForMigration() {
        assertTrue(PinSecurity.verify("123456", "123456"))
        assertFalse(PinSecurity.verify("000000", "123456"))
    }
}
