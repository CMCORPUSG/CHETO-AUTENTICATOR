package com.cmcorpusg.chetoauthenticator.backup

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import org.json.JSONObject

data class DriveBackupInfo(
    val id: String,
    val name: String,
    val modifiedTime: String
)

class DriveBackupClient(private val prefix: String = "cheto_backup_") {
    fun upload(accessToken: String, encryptedPayload: String) {
        val boundary = "cheto-${UUID.randomUUID()}"
        val metadata = JSONObject()
            .put("name", "${prefix}${System.currentTimeMillis()}.enc")
            .put("parents", org.json.JSONArray().put("appDataFolder"))
            .toString()

        val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        }

        connection.outputStream.buffered().use { out ->
            fun write(text: String) = out.write(text.encodeToByteArray())
            write("--$boundary\r\n")
            write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            write(metadata)
            write("\r\n--$boundary\r\n")
            write("Content-Type: application/octet-stream\r\n\r\n")
            write(encryptedPayload)
            write("\r\n--$boundary--\r\n")
        }

        ensureSuccess(connection)
        trimOldBackups(accessToken)
    }

    fun downloadLatest(accessToken: String): String? {
        val id = listBackups(accessToken, 1).firstOrNull()?.id ?: return null
        return download(accessToken, id)
    }

    fun download(accessToken: String, fileId: String): String {
        require(fileId.matches(Regex("[A-Za-z0-9_-]{5,}"))) { "Identificador de Drive inválido" }
        val connection = open(
            "https://www.googleapis.com/drive/v3/files/$fileId?alt=media",
            accessToken,
            "GET"
        )
        ensureSuccess(connection)
        return readBody(connection)
    }

    fun listRecentBackups(accessToken: String, limit: Int = MAX_BACKUPS): List<DriveBackupInfo> =
        listBackups(accessToken, limit.coerceIn(1, MAX_BACKUPS))

    fun delete(accessToken: String, fileId: String) {
        require(fileId.matches(Regex("[A-Za-z0-9_-]{5,}"))) { "Identificador de Drive inválido" }
        val connection = open(
            "https://www.googleapis.com/drive/v3/files/$fileId",
            accessToken,
            "DELETE"
        )
        val code = connection.responseCode
        connection.disconnect()
        if (code !in 200..299 && code != 404) {
            throw IllegalStateException("No se pudo eliminar la copia de Drive (HTTP $code)")
        }
    }

    private fun trimOldBackups(accessToken: String) {
        val files = listBackups(accessToken, 100)
        files.drop(MAX_BACKUPS).forEach { backup ->
            val connection = open(
                "https://www.googleapis.com/drive/v3/files/${backup.id}",
                accessToken,
                "DELETE"
            )
            val code = connection.responseCode
            connection.disconnect()
            if (code !in 200..299 && code != 404) return@forEach
        }
    }

    private fun listBackups(accessToken: String, pageSize: Int): List<DriveBackupInfo> {
        val query = URLEncoder.encode(
            "name contains '$prefix' and trashed = false",
            Charsets.UTF_8.name()
        )
        val fields = URLEncoder.encode("files(id,name,modifiedTime)", Charsets.UTF_8.name())
        val url = "https://www.googleapis.com/drive/v3/files" +
            "?spaces=appDataFolder&q=$query&orderBy=modifiedTime%20desc" +
            "&pageSize=$pageSize&fields=$fields"

        val connection = open(url, accessToken, "GET")
        ensureSuccess(connection)
        val array = JSONObject(readBody(connection)).optJSONArray("files") ?: return emptyList()

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    DriveBackupInfo(
                        id = item.getString("id"),
                        name = item.optString("name"),
                        modifiedTime = item.optString("modifiedTime")
                    )
                )
            }
        }
    }

    private fun open(url: String, token: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $token")
        }

    private fun ensureSuccess(connection: HttpURLConnection) {
        val code = connection.responseCode
        if (code !in 200..299) {
            val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            throw IllegalStateException("Google Drive HTTP $code: $error")
        }
    }

    private fun readBody(connection: HttpURLConnection): String =
        BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            .also { connection.disconnect() }

    companion object {
        private const val MAX_BACKUPS = 7
    }
}
