package com.cmcorpusg.chetoauthenticator.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TrashPolicyTest {
    @Test
    fun removesOnlyEntriesOlderThanRetentionWindow() {
        val day = 24L * 60L * 60L * 1000L
        val now = 100L * day
        val items = listOf(
            TrashedAccount(
                MobileAccount(id = "old", issuer = "Old", secret = "AAAA"),
                deletedAtEpochMillis = now - 31L * day
            ),
            TrashedAccount(
                MobileAccount(id = "new", issuer = "New", secret = "BBBB"),
                deletedAtEpochMillis = now - 29L * day
            )
        )

        val result = TrashPolicy.prune(items, 30, now)

        assertEquals(listOf("new"), result.map { it.account.id })
    }

    @Test
    fun manualRetentionKeepsEverything() {
        val items = listOf(
            TrashedAccount(MobileAccount(id = "old", issuer = "Old", secret = "AAAA"), 1L)
        )

        assertEquals(items, TrashPolicy.prune(items, 0, 999999999L))
    }
}
