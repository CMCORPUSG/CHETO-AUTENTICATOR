package com.cmcorpusg.chetoauthenticator.data

object VaultMergePolicy {
    fun merge(current: MobileVault, restored: MobileVault): MobileVault {
        val mergedAccounts = current.accounts.toMutableList()
        restored.accounts.forEach { candidate ->
            if (AccountPolicy.findDuplicate(candidate, mergedAccounts) == null) {
                mergedAccounts += candidate
            }
        }

        val mergedEmails = (current.emails + restored.emails)
            .fold(mutableListOf<String>()) { acc, email ->
                if (acc.none { it.equals(email, true) }) acc += email
                acc
            }

        val mergedIdentities = (current.linkedIdentities + restored.linkedIdentities)
            .distinctBy { it.provider + ":" + it.subject }

        return current.copy(
            name = current.name.ifBlank { restored.name },
            photo = current.photo.ifBlank { restored.photo },
            emails = mergedEmails,
            categories = (current.categories + restored.categories).distinct(),
            accounts = mergedAccounts,
            categoryColors = restored.categoryColors + current.categoryColors,
            linkedIdentities = mergedIdentities
        )
    }
}
