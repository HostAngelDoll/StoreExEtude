package dev.anilbeesetti.nextplayer.core.data.network

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.database.dao.JournalDao
import dev.anilbeesetti.nextplayer.core.database.entities.asEntity
import dev.anilbeesetti.nextplayer.core.model.Journal
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import dev.anilbeesetti.nextplayer.core.common.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

sealed class SyncResult {
    object Success : SyncResult()
    object ServerNotFound : SyncResult()
    object ServerNoResources : SyncResult()
    object SettingsIncomplete : SyncResult()
    data class Error(val message: String) : SyncResult()
}

@Singleton
class JournalSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val client: StoreEtudeClient,
    private val serverScanner: ServerScanner,
    private val journalDao: JournalDao
) {
    companion object {
        private const val TAG = "JournalSyncManager"
    }

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun sync(): SyncResult {
        val prefs = preferencesRepository.applicationPreferences.first()
        if (prefs.recursosUri == null || prefs.jornadasUri == null) {
            return SyncResult.SettingsIncomplete
        }

        val port = prefs.serverPort
        var ip = prefs.manualServerIp ?: prefs.lastServerIp

        if (ip == null) {
            ip = serverScanner.scan(port)
        } else {
            val ping = client.ping(ip, port)
            if (ping?.name?.trim()?.equals("StoreEtude", ignoreCase = true) != true) {
                Logger.logDebug(TAG, "Manual/Last IP $ip failed handshake: name=${ping?.name}")
                ip = serverScanner.scan(port)
            }
        }

        if (ip == null) return SyncResult.ServerNotFound

        // Cache found IP
        preferencesRepository.updateApplicationPreferences { it.copy(lastServerIp = ip) }

        val health = client.health(ip, port)
        if (health != 200) {
            return if (health == 503) SyncResult.ServerNoResources else SyncResult.Error("Server health check failed: $health")
        }

        val syncData = client.sync(ip, port) ?: return SyncResult.Error("Failed to fetch sync data")

        val remoteJournals = syncData.journals.map {
            Journal(
                id = it.id,
                name = it.nombre,
                expectedDate = try {
                    LocalDate.parse(it.fecha_esperada).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                } catch (e: Exception) {
                    0L
                },
                state = it.estado,
                materialsCount = it.materiales.size,
                updatedAt = try {
                    OffsetDateTime.parse(it.updated_at).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    0L
                },
                deleted = it.deleted
            )
        }

        // Merge Strategy
        val localEntities = journalDao.getActiveJournals().first()
        val localMap = localEntities.associateBy { it.id }

        val toUpsert = mutableListOf<Journal>()
        val remoteIds = remoteJournals.map { it.id }.toSet()

        remoteJournals.forEach { remote ->
            val local = localMap[remote.id]
            if (local == null || remote.updatedAt > local.updatedAt) {
                toUpsert.add(remote)
            }
        }

        // Remove local journals that are no longer on the server
        localEntities.forEach { local ->
            if (!remoteIds.contains(local.id)) {
                journalDao.deleteById(local.id)
            }
        }

        // Upsert new or updated journals
        journalDao.upsertJournals(toUpsert.map { it.asEntity() })

        // Save JSON to jornadasDir
        prefs.jornadasUri?.let { saveSyncDataToDisk(syncData, it) }

        return SyncResult.Success
    }

    fun showSyncResultMessage(result: SyncResult) {
        val message = when (result) {
            is SyncResult.Success -> "Sincronización exitosa"
            is SyncResult.ServerNotFound -> "Servidor no encontrado"
            is SyncResult.ServerNoResources -> "Servidor activo pero sin recursos"
            is SyncResult.SettingsIncomplete -> "Configuración incompleta"
            is SyncResult.Error -> result.message
        }
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun readSyncData(jornadasUri: String): SyncResponse? = withContext(Dispatchers.IO) {
        try {
            val treeUri = Uri.parse(jornadasUri)
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext null
            val file = root.findFile("sync_data.json") ?: return@withContext null

            context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().readText()
                json.decodeFromString<SyncResponse>(content)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateMaterialTracking(journalId: String, materialIndex: Int, datetimeRange: String) = withContext(Dispatchers.IO) {
        updateMaterial(journalId, materialIndex) { materialJson ->
            val map = materialJson.toMutableMap()
            map["datetime_range_utc_06"] = JsonPrimitive(datetimeRange)
            JsonObject(map)
        }
    }

    suspend fun updateMaterialSelection(
        journalId: String,
        materialIndex: Int,
        title: String,
        path: String,
        lyricPath: String?
    ) = withContext(Dispatchers.IO) {
        updateMaterial(journalId, materialIndex) { materialJson ->
            val map = materialJson.toMutableMap()
            map["title_material"] = JsonPrimitive(title)
            map["path"] = JsonPrimitive(path)
            if (lyricPath != null) {
                map["lyric_path"] = JsonPrimitive(lyricPath)
            }
            JsonObject(map)
        }
    }

    private suspend fun updateMaterial(
        journalId: String,
        materialIndex: Int,
        transform: (JsonObject) -> JsonObject
    ) = withContext(Dispatchers.IO) {
        val prefs = preferencesRepository.applicationPreferences.first()
        val jornadasUri = prefs.jornadasUri ?: return@withContext

        val syncData = readSyncData(jornadasUri) ?: return@withContext

        val updatedJournals = syncData.journals.map { journal ->
            if (journal.id == journalId) {
                val updatedMateriales = journal.materiales.mapIndexed { index, materialJson ->
                    if (index == materialIndex) {
                        transform(materialJson)
                    } else {
                        materialJson
                    }
                }
                journal.copy(materiales = updatedMateriales)
            } else {
                journal
            }
        }

        saveSyncDataToDisk(syncData.copy(journals = updatedJournals), jornadasUri)
    }

    private fun saveSyncDataToDisk(syncData: SyncResponse, jornadasUri: String) {
        try {
            val treeUri = Uri.parse(jornadasUri)
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return

            val filename = "sync_data.json"
            val tempFilename = "sync_data.json.tmp"

            // Create or find temp file
            var tempFile = root.findFile(tempFilename)
            if (tempFile == null) {
                tempFile = root.createFile("application/json", tempFilename)
            }

            tempFile?.let { tFile ->
                context.contentResolver.openOutputStream(tFile.uri)?.use { output ->
                    output.write(json.encodeToString(syncData).toByteArray())
                }

                // Atomic-ish replace: delete original and rename temp
                val originalFile = root.findFile(filename)
                originalFile?.delete()
                tFile.renameTo(filename)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
