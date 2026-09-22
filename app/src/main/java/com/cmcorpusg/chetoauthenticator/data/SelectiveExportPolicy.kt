package com.cmcorpusg.chetoauthenticator.data

object SelectiveExportPolicy {
    fun create(source: MobileVault, accountIds: Set<String>): MobileVault {
        val selected = source.accounts.filter { it.id in accountIds }
        require(selected.isNotEmpty()) { "Sin cuentas seleccionadas" }

        val usedCategories = (listOf("Sin categoría") + selected.map { it.category }).distinct()

        return source.copy(
            name = "",
            username = "",
            emails = emptyList(),
            verifiedEmails = emptyList(),
            photo = "",
            categories = usedCategories,
            accounts = selected,
            categoryColors = source.categoryColors.filterKeys { it in usedCategories },
            linkedIdentities = emptyList(),
            trash = emptyList()
        )
    }
}
