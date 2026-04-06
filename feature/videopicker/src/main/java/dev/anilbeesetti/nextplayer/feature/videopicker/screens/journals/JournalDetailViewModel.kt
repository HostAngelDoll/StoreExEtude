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
import dev.anilbeesetti.nextplayer.core.data.network.JournalResponse
import dev.anilbeesetti.nextplayer.core.data.network.SyncResponse
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.JournalDetailRoute
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
                    journalResponse.materiales.mapIndexed { index, materialJson ->
                        val titleMaterial = materialJson["title_material"]?.jsonPrimitive?.contentOrNull ?: ""
                        val path = materialJson["path"]?.jsonPrimitive?.contentOrNull
                        val datetimeRange = materialJson["datetime_range_utc_06"]?.jsonPrimitive?.contentOrNull ?: ""

                        val isDownloaded = if (!path.isNullOrEmpty() && recursosUri != null) {
                            checkFileExists(recursosUri, path)
                        } else {
                            false
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
                            isDownloaded = isDownloaded,
                            duration = duration,
                            hasSidecar = hasSidecar,
                            hasUserSelection = hasUserSelection,
                            isPlayed = isPlayed,
                            uri = fileUri
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

    private fun checkFileExists(recursosUri: String, path: String): Boolean {
        return try {
            val file = findFileByPath(recursosUri, path)
            file?.exists() == true
        } catch (e: Exception) {
            false
        }
    }

    private fun checkSidecarExists(recursosUri: String, path: String): Boolean {
        val basePath = path.substringBeforeLast(".")
        return checkFileExists(recursosUri, "$basePath.txt") || checkFileExists(recursosUri, "$basePath.md")
    }

    private fun getFileUri(recursosUri: String, path: String): Uri? {
        return findFileByPath(recursosUri, path)?.uri
    }

    private fun findFileByPath(recursosUri: String, path: String): DocumentFile? {
        val treeUri = Uri.parse(recursosUri)
        var currentDir = DocumentFile.fromTreeUri(context, treeUri) ?: return null

        val segments = path.split("/").filter { it.isNotEmpty() }
        for (i in 0 until segments.size - 1) {
            currentDir = currentDir.findFile(segments[i]) ?: return null
        }
        return currentDir.findFile(segments.last())
    }

    private fun getVideoDuration(recursosUri: String, path: String): String? {
        val file = findFileByPath(recursosUri, path) ?: return null
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
}
