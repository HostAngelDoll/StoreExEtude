package dev.anilbeesetti.nextplayer.core.data.network

import dev.anilbeesetti.nextplayer.core.common.Logger
import dev.anilbeesetti.nextplayer.core.model.Journal
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PingResponse(val name: String, val version: String? = null)

@Serializable
data class SyncResponse(val journals: List<JournalResponse>)

@Serializable
data class UpdateJournalRequest(
    val materiales: List<JsonObject>
)

@Serializable
data class DownloadListResponse(
    val path: String,
    val files: List<FileInfo>
)

@Serializable
data class FileInfo(
    val name: String,
    val size: Long
)

@Serializable
data class JournalResponse(
    val id: String,
    val nombre: String,
    val fecha_esperada: String,
    val estado: String,
    val materiales: List<JsonObject>,
    val updated_at: String,
    val deleted: Boolean = false
)

fun JournalResponse.asExternalModel() = dev.anilbeesetti.nextplayer.core.model.Journal(
    id = id,
    name = nombre,
    expectedDate = try {
        java.time.LocalDate.parse(fecha_esperada).atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
    } catch (e: Exception) {
        0L
    },
    state = estado,
    materialsCount = materiales.size,
    updatedAt = try {
        java.time.OffsetDateTime.parse(updated_at).toInstant().toEpochMilli()
    } catch (e: Exception) {
        0L
    },
    deleted = deleted
)

@Singleton
class StoreEtudeClient @Inject constructor() {

    companion object {
        private const val TAG = "StoreEtudeClient"
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
            connectTimeoutMillis = 2000
            socketTimeoutMillis = 5000
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    suspend fun ping(ip: String, port: Int): PingResponse? {
        val url = "http://$ip:$port/ping"
        return try {
            val response = client.get(url) {
                timeout {
                    requestTimeoutMillis = 1000
                    connectTimeoutMillis = 500
                }
            }
            val bodyText = response.bodyAsText()
            Logger.logDebug(TAG, "Ping response for $url: Status=${response.status}, Body=$bodyText")
            // Try explicit decoding if needed, though Ktor should handle it
            Json.decodeFromString<PingResponse>(bodyText)
        } catch (e: Exception) {
            Logger.logError(TAG, "Ping failed for $url: ${e.message}")
            null
        }
    }

    suspend fun health(ip: String, port: Int): Int {
        val url = "http://$ip:$port/health"
        return try {
            val response = client.get(url) {
                timeout {
                    requestTimeoutMillis = 1500
                    connectTimeoutMillis = 1500
                    socketTimeoutMillis = 1500
                }
            }
            response.status.value
        } catch (e: Exception) {
            Logger.logError(TAG, "Health check failed for $url: ${e.message}")
            -1
        }
    }

    suspend fun updateJournalMaterials(
        ip: String,
        port: Int,
        journalId: String,
        materiales: List<JsonObject>
    ): HttpResponse {
        val url = "http://$ip:$port/journal/$journalId"
        return client.put(url) {
            contentType(ContentType.Application.Json)
            setBody(UpdateJournalRequest(materiales))
            timeout {
                requestTimeoutMillis = 1500
                connectTimeoutMillis = 1500
                socketTimeoutMillis = 1500
            }
        }
    }

    suspend fun sync(ip: String, port: Int): SyncResponse? {
        val url = "http://$ip:$port/journals_sync"
        return try {
            client.get(url).body()
        } catch (e: Exception) {
            Logger.logError(TAG, "Sync failed for $url: ${e.message}")
            null
        }
    }

    suspend fun getDownloadList(ip: String, port: Int, path: String): DownloadListResponse? {
        val url = "http://$ip:$port/download/list"
        return try {
            client.get(url) {
                parameter("path", path)
            }.body()
        } catch (e: Exception) {
            Logger.logError(TAG, "Download list failed for $url?path=$path: ${e.message}")
            null
        }
    }

    suspend fun downloadFile(ip: String, port: Int, path: String): HttpStatement {
        val url = "http://$ip:$port/downloads"
        return client.prepareGet(url) {
            parameter("path", path)
            timeout {
                requestTimeoutMillis = Long.MAX_VALUE
                socketTimeoutMillis = Long.MAX_VALUE
                connectTimeoutMillis = 30000
            }
        }
    }

    suspend fun downloadFileWithProgress(
        ip: String,
        port: Int,
        path: String,
        onProgress: (Long, Long) -> Unit
    ): HttpStatement {
        val url = "http://$ip:$port/downloads"
        return client.prepareGet(url) {
            parameter("path", path)
            onDownload { bytesSentTotal, contentLength ->
                onProgress(bytesSentTotal, contentLength ?: 0L)
            }
            timeout {
                requestTimeoutMillis = Long.MAX_VALUE
                socketTimeoutMillis = Long.MAX_VALUE
                connectTimeoutMillis = 30000
            }
        }
    }
}
