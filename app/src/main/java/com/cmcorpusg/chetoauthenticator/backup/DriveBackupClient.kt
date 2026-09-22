package com.cmcorpusg.chetoauthenticator.backup

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class DriveBackupInfo(
    val id: String,
    val name: String,
    val modifiedTime: String
)

private data class DriveFolderInfo(
    val id: String,
    val webViewLink: String
)

class DriveBackupClient(private val prefix: String = "cheto_backup_") {

    fun upload(accessToken: String, encryptedPayload: String) {
        val folder = ensureBackupFolder(accessToken)
        val existing = listBackups(accessToken, 100, folder.id)
            .firstOrNull { it.name == fixedBackupName }

        if (existing != null) {
            val connection = open(
                "https://www.googleapis.com/upload/drive/v3/files/${existing.id}?uploadType=media",
                accessToken,
                "PATCH"
            ).apply {
                doOutput = true
                setRequestProperty("Content-Type", "application/octet-stream")
            }
            connection.outputStream.use { it.write(encryptedPayload.toByteArray(Charsets.UTF_8)) }
            ensureSuccess(connection)
            connection.disconnect()
        } else {
            val boundary = "cheto-${UUID.randomUUID()}"
            val metadata = JSONObject()
                .put("name", fixedBackupName)
                .put("parents", JSONArray().put(folder.id))
                .toString()

            val url = URL(
                "https://www.googleapis.com/upload/drive/v3/files" +
                    "?uploadType=multipart&fields=id,name,modifiedTime,webViewLink"
            )
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
            connection.inputStream.close()
            connection.disconnect()
        }

        removeVisibleDuplicates(accessToken, folder.id)
        removeLegacyAppDataCopies(accessToken)
    }

    fun downloadLatest(accessToken: String): String? {
        val folder = ensureBackupFolder(accessToken)
        val id = listBackups(accessToken, 1, folder.id).firstOrNull()?.id ?: return null
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

    fun listRecentBackups(accessToken: String, limit: Int = MAX_BACKUPS): List<DriveBackupInfo> {
        val folder = ensureBackupFolder(accessToken)
        return listBackups(accessToken, limit.coerceIn(1, MAX_BACKUPS), folder.id)
    }

    fun backupFolderWebUrl(accessToken: String): String {
        val folder = ensureBackupFolder(accessToken)
        return folder.webViewLink.ifBlank {
            "https://drive.google.com/drive/folders/${folder.id}"
        }
    }

    fun currentAccountEmail(accessToken: String): String? {
        val fields = URLEncoder.encode("user(emailAddress)", Charsets.UTF_8.name())
        val connection = open(
            "https://www.googleapis.com/drive/v3/about?fields=$fields",
            accessToken,
            "GET"
        )
        ensureSuccess(connection)
        return JSONObject(readBody(connection))
            .optJSONObject("user")
            ?.optString("emailAddress")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

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

    private fun ensureBackupFolder(accessToken: String): DriveFolderInfo {
        val rootFolder = findFolder(accessToken, ROOT_FOLDER_NAME, "root")
            ?: createFolder(accessToken, ROOT_FOLDER_NAME, "root")
        return findFolder(accessToken, BACKUP_FOLDER_NAME, rootFolder.id)
            ?: createFolder(accessToken, BACKUP_FOLDER_NAME, rootFolder.id)
    }

    private fun findFolder(
        accessToken: String,
        name: String,
        parentId: String
    ): DriveFolderInfo? {
        val escapedName = name.replace("'", "\\'")
        val query = URLEncoder.encode(
            "name = '$escapedName' and mimeType = '$FOLDER_MIME' and " +
                "'$parentId' in parents and trashed = false",
            Charsets.UTF_8.name()
        )
        val fields = URLEncoder.encode("files(id,webViewLink)", Charsets.UTF_8.name())
        val connection = open(
            "https://www.googleapis.com/drive/v3/files" +
                "?spaces=drive&q=$query&pageSize=10&fields=$fields",
            accessToken,
            "GET"
        )
        ensureSuccess(connection)
        val files = JSONObject(readBody(connection)).optJSONArray("files")
        val first = files?.optJSONObject(0) ?: return null
        return DriveFolderInfo(
            id = first.getString("id"),
            webViewLink = first.optString("webViewLink")
        )
    }

    private fun createFolder(
        accessToken: String,
        name: String,
        parentId: String
    ): DriveFolderInfo {
        val metadata = JSONObject()
            .put("name", name)
            .put("mimeType", FOLDER_MIME)
            .put("parents", JSONArray().put(parentId))
            .toString()

        val connection = open(
            "https://www.googleapis.com/drive/v3/files?fields=id,webViewLink",
            accessToken,
            "POST"
        ).apply {
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        connection.outputStream.use { it.write(metadata.toByteArray(Charsets.UTF_8)) }
        ensureSuccess(connection)
        val json = JSONObject(readBody(connection))
        return DriveFolderInfo(
            id = json.getString("id"),
            webViewLink = json.optString("webViewLink")
        )
    }

    private fun removeVisibleDuplicates(accessToken: String, folderId: String) {
        val files = listBackups(accessToken, 100, folderId)
        files.filter { it.name != fixedBackupName }.forEach { backup ->
            runCatching { delete(accessToken, backup.id) }
        }
    }

    private fun removeLegacyAppDataCopies(accessToken: String) {
        runCatching {
            val query = URLEncoder.encode(
                "name contains '$prefix' and trashed = false",
                Charsets.UTF_8.name()
            )
            val fields = URLEncoder.encode("files(id,name)", Charsets.UTF_8.name())
            val connection = open(
                "https://www.googleapis.com/drive/v3/files" +
                    "?spaces=appDataFolder&q=$query&pageSize=100&fields=$fields",
                accessToken,
                "GET"
            )
            ensureSuccess(connection)
            val files = JSONObject(readBody(connection)).optJSONArray("files") ?: return@runCatching
            for (index in 0 until files.length()) {
                val id = files.optJSONObject(index)?.optString("id").orEmpty()
                if (id.isBlank()) continue
                runCatching { delete(accessToken, id) }
            }
        }
    }

    private fun listBackups(
        accessToken: String,
        pageSize: Int,
        folderId: String
    ): List<DriveBackupInfo> {
        val query = URLEncoder.encode(
            "'$folderId' in parents and name contains '$prefix' and trashed = false",
            Charsets.UTF_8.name()
        )
        val fields = URLEncoder.encode("files(id,name,modifiedTime)", Charsets.UTF_8.name())
        val url = "https://www.googleapis.com/drive/v3/files" +
            "?spaces=drive&q=$query&orderBy=modifiedTime%20desc" +
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
            val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()

            val safeDetail = runCatching {
                val root = JSONObject(errorBody)
                val error = root.optJSONObject("error")
                val message = error?.optString("message").orEmpty()
                val reason = error
                    ?.optJSONArray("errors")
                    ?.optJSONObject(0)
                    ?.optString("reason")
                    .orEmpty()

                listOf(reason, message)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                    .take(240)
            }.getOrDefault("")

            throw IllegalStateException(
                "Google Drive HTTP $code" +
                    safeDetail.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
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
        private const val ROOT_FOLDER_NAME = "CHETO Authenticator"
        private const val BACKUP_FOLDER_NAME = "Backups"
        private const val FOLDER_MIME = "application/vnd.google-apps.folder"
    }
}
