package com.cmcorpusg.chetoauthenticator.ui

object ServiceCatalog {
    val suggestions = listOf(
        "Google","Microsoft","GitHub","OpenAI","Twitch","Kick",
        "Discord","Amazon","PayPal","Instagram","Facebook","Dropbox",
        "YouTube","Binance","Steam","Apple"
    )

    private val domains = linkedMapOf(
        "google" to "google.com",
        "gmail" to "gmail.com",
        "microsoft" to "microsoft.com",
        "outlook" to "outlook.com",
        "hotmail" to "hotmail.com",
        "twitch" to "twitch.tv",
        "kick" to "kick.com",
        "github" to "github.com",
        "openai" to "openai.com",
        "discord" to "discord.com",
        "paypal" to "paypal.com",
        "amazon" to "amazon.com",
        "instagram" to "instagram.com",
        "facebook" to "facebook.com",
        "dropbox" to "dropbox.com",
        "youtube" to "youtube.com",
        "coinbase" to "coinbase.com",
        "shopify" to "shopify.com",
        "tesla" to "tesla.com",
        "roblox" to "roblox.com",
        "whatsapp" to "whatsapp.com",
        "apple" to "apple.com",
        "binance" to "binance.com",
        "steam" to "steampowered.com",
        "xbox" to "xbox.com",
        "playstation" to "playstation.com"
    )

    fun domainFor(issuer: String): String? {
        val normalized = issuer.trim().lowercase()
        if (normalized.isBlank()) return null
        domains[normalized]?.let { return it }

        return domains.entries.firstOrNull { (name, _) ->
            normalized.contains(name)
        }?.value ?: normalized
            .takeIf { it.contains('.') && !it.contains(' ') }
            ?.removePrefix("https://")
            ?.removePrefix("http://")
            ?.substringBefore('/')
    }

    fun logoUrlFor(issuer: String): String? =
        domainFor(issuer)?.let { domain ->
            "https://www.google.com/s2/favicons?domain_url=https://$domain&sz=128"
        }
}
