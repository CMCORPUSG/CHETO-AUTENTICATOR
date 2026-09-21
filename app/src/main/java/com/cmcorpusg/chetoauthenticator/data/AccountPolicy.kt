package com.cmcorpusg.chetoauthenticator.data

object AccountPolicy {
    fun normalizedSecret(value: String): String =
        value.filterNot(Char::isWhitespace).uppercase()

    fun normalizedText(value: String): String =
        value.trim().lowercase()

    fun findDuplicate(
        candidate: MobileAccount,
        existing: List<MobileAccount>
    ): MobileAccount? {
        val candidateSecret = normalizedSecret(candidate.secret)
        if (candidateSecret.isBlank()) return null

        return existing.firstOrNull { account ->
            if (account.id == candidate.id) return@firstOrNull false

            val sameSecret = normalizedSecret(account.secret) == candidateSecret
            val sameIdentity =
                normalizedText(account.issuer) == normalizedText(candidate.issuer) &&
                normalizedText(account.label) == normalizedText(candidate.label)

            sameSecret || sameIdentity
        }
    }
}
