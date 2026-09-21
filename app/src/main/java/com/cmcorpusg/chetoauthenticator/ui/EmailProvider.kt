package com.cmcorpusg.chetoauthenticator.ui

object EmailProvider {
    fun nameFor(address: String): String {
        val domain = address.substringAfter('@', "").lowercase()
        return when {
            domain == "gmail.com" || domain == "googlemail.com" -> "Gmail"
            domain == "outlook.com" || domain.endsWith(".outlook.com") -> "Outlook"
            domain == "hotmail.com" || domain.endsWith(".hotmail.com") -> "Hotmail"
            domain == "live.com" -> "Microsoft"
            domain == "icloud.com" || domain == "me.com" -> "Apple"
            domain.isNotBlank() -> domain.substringBefore('.').replaceFirstChar { it.uppercase() }
            else -> "Correo"
        }
    }

    fun logoUrlFor(address: String): String? {
        val provider = nameFor(address)
        return ServiceCatalog.logoUrlFor(provider)
            ?: address.substringAfter('@', "")
                .takeIf { it.contains('.') }
                ?.let { "https://www.google.com/s2/favicons?domain_url=https://$it&sz=128" }
    }
}
