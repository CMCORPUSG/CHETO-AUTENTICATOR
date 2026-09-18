package com.cmcorpusg.chetoauthenticator.backup

import android.content.Context
import android.util.Base64
import com.cmcorpusg.chetoauthenticator.security.DeviceCrypto
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class RecoveryKeyStore(context: Context) {
    private val prefs = context.getSharedPreferences("cheto_recovery_key", Context.MODE_PRIVATE)
    private val crypto = DeviceCrypto("cheto_recovery_wrap_v1")

    fun configure(password: CharArray) {
        require(password.size >= 10) { "Use at least 10 characters for the recovery password" }
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val derived = derive(password, salt)
        val wrapped = crypto.encrypt(derived.encoded)

        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_WRAPPED, Base64.encodeToString(wrapped, Base64.NO_WRAP))
            .apply()

        password.fill('\u0000')
    }

    fun hasConfiguredKey(): Boolean =
        prefs.contains(KEY_SALT) && prefs.contains(KEY_WRAPPED)

    fun getSalt(): ByteArray? =
        prefs.getString(KEY_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) }

    fun getKey(): SecretKey? {
        val wrapped = prefs.getString(KEY_WRAPPED, null) ?: return null
        return runCatching {
            val bytes = crypto.decrypt(Base64.decode(wrapped, Base64.NO_WRAP))
            SecretKeySpec(bytes, "AES")
        }.getOrNull()
    }

    companion object {
        const val ITERATIONS = 210_000
        private const val SALT_BYTES = 16
        private const val KEY_SALT = "salt"
        private const val KEY_WRAPPED = "wrapped_key"

        fun derive(password: CharArray, salt: ByteArray): SecretKey {
            val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
            return try {
                val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
            } finally {
                spec.clearPassword()
            }
        }
    }
}
