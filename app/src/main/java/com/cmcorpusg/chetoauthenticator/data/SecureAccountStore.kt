package com.cmcorpusg.chetoauthenticator.data

import android.content.Context
import android.util.Base64
import com.cmcorpusg.chetoauthenticator.security.DeviceCrypto

class SecureAccountStore(context: Context) {
    private val prefs = context.getSharedPreferences("cheto_secure_accounts", Context.MODE_PRIVATE)
    private val crypto = DeviceCrypto("cheto_accounts_v1")

    @Synchronized
    fun load(): List<AuthAccount> {
        val value = prefs.getString(KEY_DATA, null) ?: return emptyList()
        return runCatching {
            val encrypted = Base64.decode(value, Base64.NO_WRAP)
            val json = crypto.decrypt(encrypted).decodeToString()
            AccountJsonCodec.decode(json)
        }.getOrElse { emptyList() }
    }

    @Synchronized
    fun save(accounts: List<AuthAccount>) {
        val json = AccountJsonCodec.encode(accounts)
        val encrypted = crypto.encrypt(json.encodeToByteArray())
        prefs.edit()
            .putString(KEY_DATA, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun exportPlainJson(): String = AccountJsonCodec.encode(load())

    fun importPlainJson(json: String) {
        val accounts = AccountJsonCodec.decode(json)
        save(accounts)
    }

    companion object {
        private const val KEY_DATA = "encrypted_accounts"
    }
}
