package com.cmcorpusg.chetoauthenticator.backup

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class OneDriveBackupInfo(
    val id: String,
    val name: String,
    val modifiedTime: String
)

class OneDriveBackupClient(
    private val prefix: String = "cheto_native_backup_"
) {
    fun upload(accessToken: String, content: String): OneDriveBackupInfo {
        ensureAppFolder(accessToken)
        val name = fixedBackupName
        val connection = open(
            "https://graph.microsoft.com/v1.0/me/drive/special/approot:/$name:/content",
            accessToken,
            "PUT"
        )
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/octet-stream")
        connection.outputStream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
        ensureSuccess(connection)
        val json = JSONObject(readBody(connection))
        val result = OneDriveBackupInfo(
            id = json.getString("id"),
            name = json.optString("name", name),
            modifiedTime = json.optString("lastModifiedDateTime")
        )
        removeLegacyDuplicates(accessToken)
        return result
    }

    fun downloadLatest(accessToken: String): String? {
        val latest = listBackups(accessToken, 1).firstOrNull() ?: return null
        return download(accessToken, latest.id)
    }

    fun download(accessToken: String, fileId: String): String {
        require(fileId.isNotBlank()) { "Identificador de OneDrive inválido" }
        val connection = open(
            "https://graph.microsoft.com/v1.0/me/drive/items/$fileId/content",
            accessToken,
            "GET"
        )
        ensureSuccess(connection)
        return readBody(connection)
    }

    fun listRecentBackups(accessToken: String, limit: Int = MAX_BACKUPS): List<OneDriveBackupInfo> =
        listBackups(accessToken, limit.coerceIn(1, MAX_BACKUPS))

    fun appFolderWebUrl(accessToken: String): String {
        val connection = open(
            "https://graph.microsoft.com/v1.0/me/drive/special/approot?%24select=webUrl",
            accessToken,
            "GET"
        )
        ensureSuccess(connection)
        return JSONObject(readBody(connection))
            .optString("webUrl")
            .takeIf { it.isNotBlank() }
            ?: error("OneDrive no devolvió la ubicación web de la carpeta de CHETO")
    }

    fun delete(accessToken: String, fileId: String) {
        require(fileId.isNotBlank()) { "Identificador de OneDrive inválido" }
        val connection = open(
            "https://graph.microsoft.com/v1.0/me/drive/items/$fileId",
            accessToken,
            "DELETE"
        )
        val code = connection.responseCode
        connection.disconnect()
        if (code !in 200..299 && code != 404) {
            throw IllegalStateException("No se pudo eliminar la copia de OneDrive (HTTP $code)")
        }
    }

    private fun ensureAppFolder(accessToken: String) {
        val connection = open(
            "https://graph.microsoft.com/v1.0/me/drive/special/approot",
            accessToken,
            "GET"
        )
        ensureSuccess(connection)
        connection.inputStream.close()
        connection.disconnect()
    }

    private fun removeLegacyDuplicates(accessToken: String) {
        val files = listBackups(accessToken, MAX_BACKUPS + 20)
        files.filter { it.name != fixedBackupName }.forEach {
            delete(accessToken, it.id)
        }
    }

    private fun listBackups(accessToken: String, pageSize: Int): List<OneDriveBackupInfo> {
        ensureAppFolder(accessToken)
        val connection = open(
            "https://graph.microsoft.com/v1.0/me/drive/special/approot/children" +
                "?%24select=id,name,lastModifiedDateTime&%24orderby=lastModifiedDateTime%20desc&%24top=$pageSize",
            accessToken,
            "GET"
        )
        ensureSuccess(connection)
        val root = JSONObject(readBody(connection))
        val values = root.optJSONArray("value") ?: return emptyList()
        return buildList {
            for (i in 0 until values.length()) {
                val item = values.optJSONObject(i) ?: continue
                val name = item.optString("name")
                if (!name.startsWith(prefix) || !name.endsWith(".cheto")) continue
                add(
                    OneDriveBackupInfo(
                        id = item.optString("id"),
                        name = name,
                        modifiedTime = item.optString("lastModifiedDateTime")
                    )
                )
            }
        }
    }

    private fun open(url: String, accessToken: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
        }

    private fun ensureSuccess(connection: HttpURLConnection) {
        val code = connection.responseCode
        if (code !in 200..299) {
            val error = runCatching {
                BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
            }.getOrDefault("")
            connection.disconnect()
            throw IllegalStateException(
                "Microsoft OneDrive respondió HTTP $code" +
                    error.takeIf { it.isNotBlank() }?.let { ": ${it.take(180)}" }.orEmpty()
            )
        }
    }

    private fun readBody(connection: HttpURLConnection): String =
        BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            .also { connection.disconnect() }

    private val fixedBackupName: String
        get() = "${prefix}current.cheto"

    companion object {
        private const val MAX_BACKUPS = 7
    }
}
