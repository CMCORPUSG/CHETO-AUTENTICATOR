package com.cmcorpusg.chetoauthenticator.data

import android.content.Context
import android.util.Base64
import com.cmcorpusg.chetoauthenticator.backup.BackupCrypto
import com.cmcorpusg.chetoauthenticator.core.TotpEngine
import com.cmcorpusg.chetoauthenticator.security.DeviceCrypto
import com.cmcorpusg.chetoauthenticator.security.PinSecurity
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class MobileAccount(
    val id: String = UUID.randomUUID().toString(), val issuer: String = "", val label: String = "",
    val secret: String = "", val category: String = "Sin categoría", val notes: String = "",
    val digits: Int = 6, val period: Int = 30, val algorithm: String = "SHA1", val photo: String = ""
)

data class LinkedIdentity(
    val provider: String,
    val subject: String,
    val email: String,
    val displayName: String = "",
    val photo: String = "",
    val linkedAtEpochMillis: Long = System.currentTimeMillis()
)

data class MobileVault(
    // "pin" stores a PBKDF2 verifier in persisted state. A six-digit value is
    // accepted only as a short-lived migration/input value and is normalized on write.
    val pin: String = "", val name: String = "", val emails: List<String> = emptyList(), val photo: String = "",
    val categories: List<String> = listOf("Sin categoría", "Trabajo", "Social", "Personal"),
    val accounts: List<MobileAccount> = emptyList(), val dark: Boolean = false,
    val hideCodes: Boolean = false, val biometric: Boolean = false, val screenshots: Boolean = false,
    val categoryColors: Map<String, String> = emptyMap(),
    val lockTimeoutSeconds: Int = 0,
    val linkedIdentities: List<LinkedIdentity> = emptyList()
)

class NativeVault(context: Context) {
    private val prefs = context.getSharedPreferences("cheto_html_v3", Context.MODE_PRIVATE)
    private val crypto = DeviceCrypto("cheto_html_v3")
    private val legacy = SecureAccountStore(context)

    fun exists() = prefs.contains("vault")

    fun read(): MobileVault = decode(
        JSONObject(
            crypto.decrypt(
                Base64.decode(
                    prefs.getString("vault", null) ?: error("Sin perfil"),
                    Base64.NO_WRAP
                )
            ).decodeToString()
        )
    )

    @Synchronized
    fun write(state: MobileVault) {
        val normalized = if (state.pin.matches(Regex("\\d{6}"))) {
            state.copy(pin = PinSecurity.encode(state.pin))
        } else {
            state
        }
        val encoded = Base64.encodeToString(
            crypto.encrypt(encode(normalized).toString().toByteArray()),
            Base64.NO_WRAP
        )
        check(prefs.edit().putString("vault", encoded).commit()) {
            "No se pudieron guardar los cambios"
        }
    }

    fun legacyAccounts() = legacy.load().map {
        MobileAccount(
            it.id,
            it.issuer,
            it.label,
            it.secret,
            digits = it.digits,
            period = it.period,
            algorithm = it.algorithm
        )
    }

    @Synchronized
    fun unlock(pin: String): MobileVault {
        check(System.currentTimeMillis() >= prefs.getLong("lockedUntil", 0)) {
            "Espera 30 segundos antes de reintentar"
        }

        val state = read()
        if (!PinSecurity.verify(pin, state.pin)) {
            val attempts = prefs.getInt("attempts", 0) + 1
            prefs.edit()
                .putInt("attempts", if (attempts >= 5) 0 else attempts)
                .putLong(
                    "lockedUntil",
                    if (attempts >= 5) System.currentTimeMillis() + 30_000 else 0
                )
                .commit()
            error("PIN incorrecto")
        }

        prefs.edit().putInt("attempts", 0).remove("lockedUntil").commit()

        // Transparent migration for the first native snapshot, which persisted
        // the raw six digits inside the already-encrypted vault.
        return if (!PinSecurity.isEncoded(state.pin)) {
            write(state.copy(pin = pin))
            read()
        } else {
            state
        }
    }

    companion object {
        private fun b64(value: ByteArray) = Base64.encodeToString(value, Base64.NO_WRAP)
        private fun bytes(value: String) = Base64.decode(value, Base64.NO_WRAP)

        fun encode(s: MobileVault): JSONObject = JSONObject().put("version", 6).put("pin", s.pin)
            .put(
                "profile",
                JSONObject()
                    .put("name", s.name)
                    .put("photo", s.photo)
                    .put("primaryEmail", s.emails.firstOrNull().orEmpty())
                    .put(
                        "emails",
                        JSONArray().apply {
                            s.emails.forEach { put(JSONObject().put("email", it)) }
                        }
                    )
                    .put(
                        "linkedIdentities",
                        JSONArray().apply {
                            s.linkedIdentities.forEach { identity ->
                                put(
                                    JSONObject()
                                        .put("provider", identity.provider)
                                        .put("subject", identity.subject)
                                        .put("email", identity.email)
                                        .put("displayName", identity.displayName)
                                        .put("photo", identity.photo)
                                        .put("linkedAt", identity.linkedAtEpochMillis)
                                )
                            }
                        }
                    )
            )
            .put(
                "categories",
                JSONArray().apply {
                    s.categories.forEach { put(JSONObject().put("name", it)) }
                }
            )
            .put(
                "categoryColors",
                JSONObject().apply {
                    s.categoryColors.forEach { (name, color) -> put(name, color) }
                }
            )
            .put(
                "settings",
                JSONObject()
                    .put("dark", s.dark)
                    .put("hide", s.hideCodes)
                    .put("biometric", s.biometric)
                    .put("screenshots", s.screenshots)
                    .put("lockTimeoutSeconds", s.lockTimeoutSeconds)
            )
            .put(
                "accounts",
                JSONArray().apply {
                    s.accounts.forEach { a ->
                        put(
                            JSONObject()
                                .put("id", a.id)
                                .put("issuer", a.issuer)
                                .put("label", a.label)
                                .put("secret", a.secret)
                                .put("category", a.category)
                                .put("notes", a.notes)
                                .put("digits", a.digits)
                                .put("period", a.period)
                                .put("alg", a.algorithm)
                                .put("logoUrl", a.photo)
                        )
                    }
                }
            )

        fun decode(root: JSONObject): MobileVault {
            val version = root.optInt("version", 1)
            val p = root.optJSONObject("profile") ?: JSONObject()
            val settings = root.optJSONObject("settings") ?: JSONObject()
            val arr = root.getJSONArray("accounts")
            require(arr.length() <= 2000) { "Demasiadas cuentas" }

            val cats = root.optJSONArray("categories") ?: JSONArray()
            val categories = (
                listOf("Sin categoría") +
                    (0 until cats.length()).map { cats.getJSONObject(it).getString("name") }
                ).distinct()

            val emails = p.optJSONArray("emails") ?: JSONArray()
            val identitiesArray = p.optJSONArray("linkedIdentities") ?: JSONArray()
            val linkedIdentities = (0 until identitiesArray.length()).mapNotNull { index ->
                val item = identitiesArray.optJSONObject(index) ?: return@mapNotNull null
                val provider = item.optString("provider").trim().lowercase()
                val subject = item.optString("subject").trim()
                val email = item.optString("email").trim()
                if (provider !in setOf("google", "microsoft") || subject.isBlank() || email.isBlank()) {
                    null
                } else {
                    LinkedIdentity(
                        provider = provider,
                        subject = subject,
                        email = email,
                        displayName = item.optString("displayName"),
                        photo = item.optString("photo"),
                        linkedAtEpochMillis = item.optLong("linkedAt", 0L).coerceAtLeast(0L)
                    )
                }
            }.distinctBy { it.provider + ":" + it.subject }
            val colorsObject = root.optJSONObject("categoryColors") ?: JSONObject()
            val categoryColors = buildMap<String, String> {
                colorsObject.keys().forEach { key ->
                    val value = colorsObject.optString(key)
                    if (value.matches(Regex("#[0-9A-Fa-f]{6}"))) put(key, value.uppercase())
                }
            }
            val accounts = (0 until arr.length()).map { i ->
                val a = arr.getJSONObject(i)
                MobileAccount(
                    a.optString("id", UUID.randomUUID().toString()),
                    a.getString("issuer"),
                    a.getString("label"),
                    a.getString("secret"),
                    a.optString("category", "Sin categoría"),
                    a.optString("notes"),
                    a.optInt("digits", 6),
                    a.optInt("period", 30),
                    a.optString("alg", "SHA1"),
                    a.optString("logoUrl")
                ).also {
                    require(it.digits in 6..8 && it.period in 1..300)
                    TotpEngine.generate(
                        it.secret,
                        digits = it.digits,
                        period = it.period,
                        algorithm = it.algorithm
                    )
                }
            }

            return MobileVault(
                root.optString("pin"),
                p.optString("name"),
                (0 until emails.length())
                    .map { emails.getJSONObject(it).getString("email") }
                    .sortedBy { if (it == p.optString("primaryEmail")) 0 else 1 },
                p.optString("photo"),
                categories,
                accounts,
                settings.optBoolean("dark"),
                settings.optBoolean("hide"),
                if (version >= 4) settings.optBoolean("biometric", false) else false,
                settings.optBoolean("screenshots"),
                categoryColors,
                settings.optInt("lockTimeoutSeconds", 0).takeIf { it in setOf(0, 30, 60, 300) } ?: 0,
                linkedIdentities
            )
        }

        private fun key(password: String, salt: ByteArray): SecretKeySpec {
            val spec = PBEKeySpec(password.toCharArray(), salt, 150000, 256)
            return try {
                SecretKeySpec(
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                        .generateSecret(spec)
                        .encoded,
                    "AES"
                )
            } finally {
                spec.clearPassword()
            }
        }

        fun exportPortableJson(s: MobileVault): String =
            encode(s).apply {
                remove("pin")
                remove("settings")
            }.toString()

        fun export(s: MobileVault, password: String): String {
            require(password.length >= 10)
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.ENCRYPT_MODE,
                key(password, salt),
                GCMParameterSpec(128, iv)
            )
            val payload = exportPortableJson(s)
            return JSONObject()
                .put("v", 1)
                .put("alg", "AES-256-GCM")
                .put("iterations", 150000)
                .put("salt", b64(salt))
                .put("iv", b64(iv))
                .put("data", b64(cipher.doFinal(payload.toByteArray())))
                .toString()
        }

        fun restore(data: String, password: String, current: MobileVault): MobileVault {
            val obj = JSONObject(data)
            val plain = if (obj.optString("format") == "CHETO-BACKUP") {
                BackupCrypto.decryptWithPassword(data, password.toCharArray())
            } else {
                require(
                    obj.getInt("v") == 1 &&
                        obj.getInt("iterations") == 150000 &&
                        obj.getString("alg") == "AES-256-GCM"
                )
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    key(password, bytes(obj.getString("salt"))),
                    GCMParameterSpec(128, bytes(obj.getString("iv")))
                )
                cipher.doFinal(bytes(obj.getString("data"))).decodeToString()
            }
            val restored = decode(JSONObject(plain))
            return current.copy(
                name = restored.name,
                emails = restored.emails,
                photo = restored.photo,
                categories = restored.categories,
                accounts = restored.accounts,
                categoryColors = restored.categoryColors,
                linkedIdentities = restored.linkedIdentities
            )
        }
    }
}
