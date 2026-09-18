package com.cmcorpusg.chetoauthenticator.backup

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

object BackupCrypto {
    fun encrypt(plainJson: String, key: SecretKey, salt: ByteArray): String {
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plainJson.encodeToByteArray())

        return JSONObject()
            .put("format", "CHETO-BACKUP")
            .put("version", 1)
            .put("kdf", "PBKDF2-HMAC-SHA256")
            .put("iterations", RecoveryKeyStore.ITERATIONS)
            .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .toString()
    }

    fun decryptWithPassword(envelope: String, password: CharArray): String {
        val root = JSONObject(envelope)
        require(root.getString("format") == "CHETO-BACKUP") { "Unknown backup format" }
        require(root.getInt("version") == 1) { "Unsupported backup version" }
        require(root.getInt("iterations") == RecoveryKeyStore.ITERATIONS) {
            "Unsupported recovery KDF parameters"
        }

        val salt = Base64.decode(root.getString("salt"), Base64.NO_WRAP)
        val iv = Base64.decode(root.getString("iv"), Base64.NO_WRAP)
        val encrypted = Base64.decode(root.getString("data"), Base64.NO_WRAP)
        val key = RecoveryKeyStore.derive(password, salt)

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted).decodeToString()
        } finally {
            password.fill('\u0000')
        }
    }
}
