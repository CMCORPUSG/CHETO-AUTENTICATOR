package com.cmcorpusg.chetoauthenticator.backup

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

object BackupCrypto {
    private const val CURRENT_VERSION = 2
    private const val MAX_DECOMPRESSED_BYTES = 32 * 1024 * 1024

    fun encrypt(plainJson: String, key: SecretKey, salt: ByteArray): String {
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val compressed = compressUtf8(plainJson)
        val encrypted = cipher.doFinal(compressed)

        return JSONObject()
            .put("format", "CHETO-BACKUP")
            .put("version", CURRENT_VERSION)
            .put("compression", "gzip")
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
        val version = root.getInt("version")
        require(version in 1..CURRENT_VERSION) { "Unsupported backup version" }
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
            val decrypted = cipher.doFinal(encrypted)
            if (version >= 2 && root.optString("compression") == "gzip") {
                decompressUtf8(decrypted)
            } else {
                decrypted.decodeToString()
            }
        } finally {
            password.fill('\u0000')
        }
    }

    fun compressUtf8(value: String): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { gzip ->
            gzip.write(value.toByteArray(Charsets.UTF_8))
        }
        return output.toByteArray()
    }

    fun decompressUtf8(value: ByteArray): String {
        val output = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(value)).use { gzip ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = gzip.read(buffer)
                if (count < 0) break
                require(output.size() + count <= MAX_DECOMPRESSED_BYTES) {
                    "Backup descomprimido demasiado grande"
                }
                output.write(buffer, 0, count)
            }
        }
        return output.toByteArray().decodeToString()
    }
}
