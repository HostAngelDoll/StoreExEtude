package dev.anilbeesetti.nextplayer.core.data.network

import dev.anilbeesetti.nextplayer.core.model.Journal
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PingResponse(val name: String, val version: String? = null)

@Serializable
data class SyncResponse(val journals: List<JournalResponse>)

@Serializable
data class JournalResponse(
    val id: String,
    val nombre: String,
    val fecha_esperada: Long,
    val estado: String,
    val materiales: List<String>,
    val updated_at: Long,
    val deleted: Boolean = false
)

@Singleton
class StoreEtudeClient @Inject constructor() {
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
        return try {
            client.get("http://$ip:$port/ping") {
                timeout {
                    requestTimeoutMillis = 1000
                    connectTimeoutMillis = 500
                }
            }.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun health(ip: String, port: Int): Int {
        return try {
            val response = client.get("http://$ip:$port/health")
            response.status.value
        } catch (e: Exception) {
            -1
        }
    }

    suspend fun sync(ip: String, port: Int): SyncResponse? {
        return try {
            client.get("http://$ip:$port/journals_sync").body()
        } catch (e: Exception) {
            null
        }
    }
}
