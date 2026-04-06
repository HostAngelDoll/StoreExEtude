package dev.anilbeesetti.nextplayer.feature.videopicker.screens.journals

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.anilbeesetti.nextplayer.core.common.extensions.findFileByPath
import dev.anilbeesetti.nextplayer.core.common.extensions.getOrCreateFileByPath
import dev.anilbeesetti.nextplayer.core.data.network.JournalResponse
import dev.anilbeesetti.nextplayer.core.data.network.ServerScanner
import dev.anilbeesetti.nextplayer.core.data.network.StoreEtudeClient
import dev.anilbeesetti.nextplayer.core.data.network.SyncResponse
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.JournalDetailRoute
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.copyAndClose
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class JournalDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val client: StoreEtudeClient,
    private val serverScanner: ServerScanner,
) : ViewModel() {

    private val journalId: String = savedStateHandle.get<String>("journalId") ?: ""

    private val _uiState = MutableStateFlow(JournalDetailUiState())
    val uiState: StateFlow<JournalDetailUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        loadJournalDetail()
    }

    fun loadJournalDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val prefs = preferencesRepository.applicationPreferences.first()
                val jornadasUri = prefs.jornadasUri
                val recursosUri = prefs.recursosUri

                if (jornadasUri == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Jornadas URI not configured") }
                    return@launch
                }

                val syncData = readSyncData(jornadasUri)
                val journalResponse = syncData?.journals?.find { it.id == journalId }

                if (journalResponse == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Journal not found") }
                    return@launch
                }

                val materials = withContext(Dispatchers.IO) {
                    val port = prefs.serverPort
                    val ip = prefs.manualServerIp ?: prefs.lastServerIp

                    journalResponse.materiales.mapIndexed { index, materialJson ->
                        val titleMaterial = materialJson["title_material"]?.jsonPrimitive?.contentOrNull ?: ""
                        val path = materialJson["path"]?.jsonPrimitive?.contentOrNull
                        val summonPath = materialJson["summon_path"]?.jsonPrimitive?.contentOrNull
                        val datetimeRange = materialJson["datetime_range_utc_06"]?.jsonPrimitive?.contentOrNull ?: ""

                        var isDownloaded = false
                        var missingFilesCount = 0

                        if (!path.isNullOrEmpty() && recursosUri != null) {
                            isDownloaded = checkFileExists(recursosUri, path)
                        } else if (!summonPath.isNullOrEmpty() && recursosUri != null) {
                            if (ip != null) {
                                val downloadList = client.getDownloadList(ip, port, summonPath)
                                if (downloadList != null) {
                                    val missingFiles = downloadList.files.filter { fileInfo ->
                                        val localPath = joinPaths(downloadList.path, fileInfo.name)
                                        !checkFileExists(recursosUri, localPath, fileInfo.size)
                                    }
                                    missingFilesCount = missingFiles.size
                                    isDownloaded = missingFilesCount == 0
                                }
                            }
                        }

                        val hasSidecar = if (isDownloaded && !path.isNullOrEmpty() && recursosUri != null) {
                            checkSidecarExists(recursosUri, path)
                        } else {
                            false
                        }

                        val duration = if (isDownloaded && !path.isNullOrEmpty() && recursosUri != null) {
                            getVideoDuration(recursosUri, path)
                        } else {
                            null
                        }

                        val hasUserSelection = titleMaterial != "[User selection]"
                        val isPlayed = datetimeRange.isNotEmpty()

                        val fileUri = if (isDownloaded && !path.isNullOrEmpty() && recursosUri != null) {
                            getFileUri(recursosUri, path)
                        } else {
                            null
                        }

                        MaterialUiModel(
                            index = index,
                            title = if (hasUserSelection) titleMaterial else "No seleccionado",
                            originalFileName = path?.substringAfterLast('/'),
                            path = path,
                            summonPath = summonPath,
                            isDownloaded = isDownloaded,
                            duration = duration,
                            hasSidecar = hasSidecar,
                            hasUserSelection = hasUserSelection,
                            isPlayed = isPlayed,
                            uri = fileUri,
                            missingFilesCount = missingFilesCount
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        journalId = journalResponse.id,
                        name = journalResponse.nombre,
                        expectedDate = try {
                            LocalDate.parse(journalResponse.fecha_esperada).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                        } catch (e: Exception) {
                            0L
                        },
                        status = journalResponse.estado,
                        updatedAt = try {
                            OffsetDateTime.parse(journalResponse.updated_at).toInstant().toEpochMilli()
                        } catch (e: Exception) {
                            0L
                        },
                        materials = materials,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun readSyncData(jornadasUri: String): SyncResponse? = withContext(Dispatchers.IO) {
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

    private fun checkFileExists(recursosUri: String, path: String, expectedSize: Long? = null): Boolean {
        return try {
            val treeUri = Uri.parse(recursosUri)
            val file = treeUri.findFileByPath(context, path)
            if (file?.exists() == true) {
                if (expectedSize != null && expectedSize > 0) {
                    file.length() == expectedSize
                } else {
                    true
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun joinPaths(base: String, name: String): String {
        return "${base.trimEnd('/')}/${name.trimStart('/')}"
    }

    private fun checkSidecarExists(recursosUri: String, path: String): Boolean {
        val basePath = path.substringBeforeLast(".")
        return checkFileExists(recursosUri, "$basePath.txt") || checkFileExists(recursosUri, "$basePath.md")
    }

    private fun getFileUri(recursosUri: String, path: String): Uri? {
        val treeUri = Uri.parse(recursosUri)
        return treeUri.findFileByPath(context, path)?.uri
    }

    private fun getVideoDuration(recursosUri: String, path: String): String? {
        val treeUri = Uri.parse(recursosUri)
        val file = treeUri.findFileByPath(context, path) ?: return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, file.uri)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val timeMs = time?.toLong() ?: 0L
            val hours = timeMs / 3600000
            val minutes = (timeMs % 3600000) / 60000
            val seconds = (timeMs % 60000) / 1000
            if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    fun resetJournal() {
        _uiState.update { state ->
            state.copy(
                materials = state.materials.map { material ->
                    material.copy(
                        isPlayed = false,
                        title = if (material.path.isNullOrEmpty()) "No seleccionado" else material.title,
                        hasUserSelection = !material.path.isNullOrEmpty()
                    )
                }
            )
        }
    }

    fun executeJournal(onPlay: (Uri) -> Unit) {
        val firstPlayable = _uiState.value.materials.firstOrNull { it.isDownloaded && !it.path.isNullOrEmpty() && it.uri != null }
        firstPlayable?.uri?.let { onPlay(it) }
    }

    fun downloadMaterials() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, downloadProgress = 0f) }
            try {
                val prefs = preferencesRepository.applicationPreferences.first()
                val recursosUri = prefs.recursosUri ?: throw Exception("Recursos URI not configured")
                val port = prefs.serverPort
                var ip = prefs.manualServerIp ?: prefs.lastServerIp

                if (ip == null) {
                    ip = serverScanner.scan(port)
                }

                if (ip == null) throw Exception("Server not found")

                val materialsToDownload = _uiState.value.materials.filter {
                    !it.isDownloaded && (it.hasUserSelection || !it.summonPath.isNullOrEmpty())
                }

                var downloadedCount = 0
                val totalMaterials = materialsToDownload.size

                for (material in materialsToDownload) {
                    if (!material.path.isNullOrEmpty()) {
                        downloadSingleFile(ip, port, material.path, recursosUri)
                    } else if (!material.summonPath.isNullOrEmpty()) {
                        val downloadList = client.getDownloadList(ip, port, material.summonPath)
                        if (downloadList != null) {
                            for (fileInfo in downloadList.files) {
                                val localPath = joinPaths(downloadList.path, fileInfo.name)
                                if (!checkFileExists(recursosUri, localPath, fileInfo.size)) {
                                    downloadSingleFile(ip, port, localPath, recursosUri)
                                }
                            }
                        }
                    }
                    downloadedCount++
                    _uiState.update { it.copy(downloadProgress = downloadedCount.toFloat() / totalMaterials) }
                }

                loadJournalDetail() // Refresh state
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isDownloading = false) }
            }
        }
    }

    private suspend fun downloadSingleFile(ip: String, port: Int, path: String, recursosUri: String) = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(recursosUri)
        val file = treeUri.getOrCreateFileByPath(context, path) ?: throw Exception("Failed to create file: $path")

        client.downloadFile(ip, port, path).execute { response ->
            context.contentResolver.openOutputStream(file.uri)?.use { output ->
                response.bodyAsChannel().toInputStream().copyTo(output)
            }
        }
    }
}
