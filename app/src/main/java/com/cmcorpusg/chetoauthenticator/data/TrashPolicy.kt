package com.cmcorpusg.chetoauthenticator.data

object TrashPolicy {
    fun prune(
        items: List<TrashedAccount>,
        retentionDays: Int,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): List<TrashedAccount> {
        if (retentionDays <= 0 || items.isEmpty()) return items
        val cutoff = nowEpochMillis - retentionDays.toLong() * 24L * 60L * 60L * 1000L
        return items.filter {
            it.deletedAtEpochMillis <= 0L || it.deletedAtEpochMillis >= cutoff
        }
    }
}
