package com.cmcorpusg.chetoauthenticator.data

import org.json.JSONArray
import org.json.JSONObject

object AccountJsonCodec {
    fun encode(accounts: List<AuthAccount>): String {
        val array = JSONArray()
        accounts.forEach { account ->
            array.put(
                JSONObject()
                    .put("id", account.id)
                    .put("issuer", account.issuer)
                    .put("label", account.label)
                    .put("secret", account.secret)
                    .put("digits", account.digits)
                    .put("period", account.period)
                    .put("algorithm", account.algorithm)
            )
        }
        return JSONObject()
            .put("schemaVersion", 1)
            .put("accounts", array)
            .toString()
    }

    fun decode(json: String): List<AuthAccount> {
        if (json.isBlank()) return emptyList()
        val root = JSONObject(json)
        require(root.optInt("schemaVersion", 1) == 1) { "Unsupported backup schema" }

        val array = root.optJSONArray("accounts") ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    AuthAccount(
                        id = item.getString("id"),
                        issuer = item.getString("issuer"),
                        label = item.getString("label"),
                        secret = item.getString("secret"),
                        digits = item.optInt("digits", 6),
                        period = item.optInt("period", 30),
                        algorithm = item.optString("algorithm", "SHA1")
                    )
                )
            }
        }
    }
}
