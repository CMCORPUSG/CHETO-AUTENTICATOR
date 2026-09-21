package com.cmcorpusg.chetoauthenticator.data

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AccountPolicyTest {
    @Test
    fun detectsDuplicateSecretIgnoringSpacesAndCase() {
        val existing = listOf(
            MobileAccount(
                id = "1",
                issuer = "GitHub",
                label = "me@example.com",
                secret = "JBSW Y3DP EH PK3PXP"
            )
        )

        val duplicate = AccountPolicy.findDuplicate(
            MobileAccount(
                id = "2",
                issuer = "Otro",
                label = "otra",
                secret = "jbswy3dpehpk3pxp"
            ),
            existing
        )

        assertNotNull(duplicate)
    }

    @Test
    fun detectsDuplicateServiceAndAccount() {
        val existing = listOf(
            MobileAccount(id = "1", issuer = "Google", label = "me@example.com", secret = "AAAA")
        )

        val duplicate = AccountPolicy.findDuplicate(
            MobileAccount(id = "2", issuer = " google ", label = "ME@example.com", secret = "BBBB"),
            existing
        )

        assertNotNull(duplicate)
    }

    @Test
    fun ignoresTheAccountBeingEdited() {
        val existing = listOf(
            MobileAccount(id = "1", issuer = "Google", label = "me@example.com", secret = "AAAA")
        )

        val duplicate = AccountPolicy.findDuplicate(
            MobileAccount(id = "1", issuer = "Google", label = "me@example.com", secret = "AAAA"),
            existing
        )

        assertNull(duplicate)
    }

    @Test
    fun allowsDistinctAccounts() {
        val existing = listOf(
            MobileAccount(id = "1", issuer = "Google", label = "one@example.com", secret = "AAAA")
        )

        val duplicate = AccountPolicy.findDuplicate(
            MobileAccount(id = "2", issuer = "GitHub", label = "two@example.com", secret = "BBBB"),
            existing
        )

        assertNull(duplicate)
    }
}
