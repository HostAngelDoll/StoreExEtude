package dev.anilbeesetti.nextplayer.feature.videopicker.screens.journals

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.anilbeesetti.nextplayer.core.common.Logger
import dev.anilbeesetti.nextplayer.core.common.ThumbnailGenerator
import dev.anilbeesetti.nextplayer.core.common.extensions.findFileByPath
import dev.anilbeesetti.nextplayer.core.common.extensions.getOrCreateFileByPath
import dev.anilbeesetti.nextplayer.core.database.dao.MediumStateDao
import dev.anilbeesetti.nextplayer.core.data.network.DownloadService
import dev.anilbeesetti.nextplayer.core.data.network.JournalResponse
import dev.anilbeesetti.nextplayer.core.data.network.JournalSyncManager
import dev.anilbeesetti.nextplayer.core.data.network.ServerScanner
import dev.anilbeesetti.nextplayer.core.data.network.StoreEtudeClient
import dev.anilbeesetti.nextplayer.core.data.network.SyncResponse
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.JournalDetailRoute
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.copyAndClose
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class JournalDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val client: StoreEtudeClient,
    private val serverScanner: ServerScanner,
    private val journalSyncManager: JournalSyncManager,
    private val mediumStateDao: MediumStateDao,
) : ViewModel() {

    private val journalId: String = savedStateHandle.get<String>("journalId") ?: ""

    private val _uiState = MutableStateFlow(JournalDetailUiState())
    val uiState: StateFlow<JournalDetailUiState> = _uiState.asStateFlow()

    private var hasReturnedFromPlayback = false

    private val json = Json { ignoreUnknownKeys = true }

    private var verificationJob: Job? = null
    private var downloadService: DownloadService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as DownloadService.LocalBinder
            downloadService = binder.getService()
            observeDownloadProgress()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            downloadService = null
        }
    }

    init {
        loadJournalDetail()
        bindDownloadService()
    }

    private fun bindDownloadService() {
        val intent = Intent(context, DownloadService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun observeDownloadProgress() {
        viewModelScope.launch {
            downloadService?.progress?.collect { progress ->
                val wasDownloading = _uiState.value.isDownloading
                _uiState.update {
                    it.copy(
                        isDownloading = progress.isDownloading,
                        currentFileName = progress.currentFileName,
                        fileProgress = progress.fileProgress,
                        overallProgress = progress.overallProgress
                    )
                }
                if (wasDownloading && !progress.isDownloading) {
                    viewModelScope.launch {
                        _uiState.update { it.copy(isLoading = true) }
                        refreshJournalDetail(isDeepCheck = false)
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        context.unbindService(serviceConnection)
    }

    fun loadJournalDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            refreshJournalDetail(isDeepCheck = false)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun refreshJournalDetail(isDeepCheck: Boolean, reportProgress: Boolean = false) {
        try {
            val prefs = preferencesRepository.applicationPreferences.first()
            val jornadasUri = prefs.jornadasUri
            val recursosUri = prefs.recursosUri

            if (jornadasUri == null) {
                _uiState.update { it.copy(error = "Jornadas URI not configured") }
                return
            }

            val syncData = readSyncData(jornadasUri)
            val journalResponse = syncData?.journals?.find { it.id == journalId }

            if (journalResponse == null) {
                _uiState.update { it.copy(error = "Journal not found") }
                return
            }

            val materials = withContext(Dispatchers.IO) {
                val port = prefs.serverPort
                val ip = prefs.manualServerIp ?: prefs.lastServerIp

                journalResponse.materiales.mapIndexed { index, materialJson ->
                    val titleMaterial = materialJson["title_material"]?.jsonPrimitive?.contentOrNull ?: ""
                    val isUserSelectionPlaceholder = titleMaterial.equals("[User selection]", ignoreCase = true)
                    val path = materialJson["path"]?.jsonPrimitive?.contentOrNull
                    val lyricPath = materialJson["lyric_path"]?.jsonPrimitive?.contentOrNull
                    val summonPath = materialJson["summon_path"]?.jsonPrimitive?.contentOrNull
                    val lyricSummonPath = materialJson["lyric_summon_path"]?.jsonPrimitive?.contentOrNull
                    val datetimeRange = materialJson["datetime_range_utc_06"]?.jsonPrimitive?.contentOrNull ?: ""

                    if (reportProgress) {
                        _uiState.update {
                            it.copy(
                                currentFileName = titleMaterial.ifEmpty { path?.substringAfterLast('/') ?: "Verificando..." },
                                overallProgress = index.toFloat() / journalResponse.materiales.size,
                                fileProgress = 0f
                            )
                        }
                    }

                    var isDownloaded = false
                    var missingFilesCount = 0
                    val isUnselected = isUserSelectionPlaceholder

                    if (recursosUri != null) {
                        if (!path.isNullOrEmpty()) {
                            // Material with specific path - Path and summon_path are mutually exclusive
                            isDownloaded = checkFileExists(recursosUri, path)
                            missingFilesCount = if (isDownloaded) 0 else 1
                        } else if (isUnselected) {
                            // Unselected material - Local validation only
                            val summonPathValid = if (!summonPath.isNullOrEmpty()) {
                                checkFolderHasValidFiles(recursosUri, summonPath)
                            } else true

                            val lyricPathValid = if (!lyricSummonPath.isNullOrEmpty()) {
                                checkFolderHasValidFiles(recursosUri, lyricSummonPath, listOf(".txt", ".md"))
                            } else true

                            isDownloaded = summonPathValid && lyricPathValid
                            missingFilesCount = if (isDownloaded) 0 else 1
                        } else {
                            // Other folder-based materials (e.g., soundtracks)
                            if (isDeepCheck) {
                                if (!summonPath.isNullOrEmpty()) {
                                    if (ip != null) {
                                        val downloadList = client.getDownloadList(ip, port, summonPath)
                                        if (downloadList != null) {
                                            val missingFiles = downloadList.files.filter { fileInfo ->
                                                val localPath = joinPaths(downloadList.path, fileInfo.name)
                                                !checkFileExists(recursosUri, localPath, fileInfo.size)
                                            }
                                            missingFilesCount += missingFiles.size
                                        } else {
                                            missingFilesCount += 1
                                        }
                                    } else {
                                        missingFilesCount += 1
                                    }
                                }

                                if (!lyricSummonPath.isNullOrEmpty()) {
                                    if (ip != null) {
                                        val downloadList = client.getDownloadList(ip, port, lyricSummonPath)
                                        if (downloadList != null) {
                                            val missingLyrics = downloadList.files.filter { fileInfo ->
                                                if (fileInfo.name.lowercase().endsWith(".txt") || fileInfo.name.lowercase().endsWith(".md")) {
                                                    val localPath = joinPaths(downloadList.path, fileInfo.name)
                                                    !checkFileExists(recursosUri, localPath, fileInfo.size)
                                                } else {
                                                    false
                                                }
                                            }
                                            missingFilesCount += missingLyrics.size
                                        } else {
                                            missingFilesCount += 1
                                        }
                                    } else {
                                        missingFilesCount += 1
                                    }
                                }
                                isDownloaded = missingFilesCount == 0 && (!summonPath.isNullOrEmpty() || !lyricSummonPath.isNullOrEmpty())
                            } else {
                                // Shallow check (initial load) - NO network calls for folders
                                isDownloaded = false
                                missingFilesCount = 1
                            }
                        }
                    }

                    val hasSidecar = if (isDownloaded && !path.isNullOrEmpty() && recursosUri != null) {
                        checkSidecarExists(recursosUri, path) || (!lyricPath.isNullOrEmpty() && checkFileExists(recursosUri, lyricPath))
                    } else {
                        false
                    }

                    val duration = if (isDownloaded && !path.isNullOrEmpty() && recursosUri != null) {
                        getVideoDuration(recursosUri, path)
                    } else {
                        null
                    }

                    val hasUserSelection = !isUserSelectionPlaceholder
                    val isPlayed = datetimeRange.isNotEmpty() && !datetimeRange.endsWith("-")
                    val isStarted = datetimeRange.endsWith("-")

                    val fileUri = if (isDownloaded && !path.isNullOrEmpty() && recursosUri != null) {
                        getFileUri(recursosUri, path)
                    } else {
                        null
                    }

                    val thumbnailUri = if (hasUserSelection && isDownloaded && fileUri != null && isVideo(path ?: "")) {
                        ThumbnailGenerator.getThumbnail(context, fileUri)
                    } else {
                        null
                    }

                    MaterialUiModel(
                        index = index,
                        title = if (hasUserSelection) titleMaterial else "No seleccionado",
                        originalFileName = path?.substringAfterLast('/'),
                        path = path,
                        summonPath = summonPath,
                        lyricSummonPath = lyricSummonPath,
                        isDownloaded = isDownloaded,
                        duration = duration,
                        hasSidecar = hasSidecar,
                        hasUserSelection = hasUserSelection,
                        isPlayed = isPlayed,
                        isStarted = isStarted,
                        uri = fileUri,
                        thumbnailUri = thumbnailUri,
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
                    overallProgress = 1f
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
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
                val size = file.length()
                if (size == 0L) return false
                if (expectedSize != null && expectedSize > 0) {
                    size == expectedSize
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

    private fun checkFolderHasValidFiles(recursosUri: String, path: String, extensions: List<String>? = null): Boolean {
        return try {
            val treeUri = Uri.parse(recursosUri)
            val folder = treeUri.findFileByPath(context, path)
            if (folder?.isDirectory == true) {
                folder.listFiles().any { file ->
                    !file.isDirectory && file.length() > 0 && (extensions == null || extensions.any { ext -> file.name?.lowercase()?.endsWith(ext) == true })
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

    fun executeJournal(onPlay: (Uri, String, Int) -> Unit) {
        val firstPlayable = _uiState.value.materials.firstOrNull { !it.isPlayed }
        firstPlayable?.let { material ->
            if (material.hasUserSelection) {
                if (material.isDownloaded && material.uri != null) {
                    onPlaybackStarted()
                    onPlay(material.uri, journalId, material.index)
                }
            } else {
                showSummonDialog(material.index)
            }
        }
    }

    private fun showSummonDialog(index: Int) {
        viewModelScope.launch {
            val material = _uiState.value.materials.getOrNull(index) ?: return@launch
            val summonPath = material.summonPath ?: return@launch

            val prefs = preferencesRepository.applicationPreferences.first()
            val recursosUri = prefs.recursosUri ?: return@launch
            val treeUri = Uri.parse(recursosUri)
            val folder = treeUri.findFileByPath(context, summonPath)

            if (folder?.isDirectory == true) {
                val files = withContext(Dispatchers.IO) {
                    folder.listFiles()
                        .filter { it.isFile && isVideo(it.name ?: "") }
                        .map { file ->
                            val isWatched = mediumStateDao.get(file.uri.toString()) != null
                            val thumbnailUri = ThumbnailGenerator.getThumbnail(context, file.uri)
                            SummonFile(
                                name = file.name ?: "Unknown",
                                uri = file.uri,
                                thumbnailUri = thumbnailUri,
                                path = joinPaths(summonPath, file.name ?: ""),
                                isWatched = isWatched
                            )
                        }
                }
                _uiState.update { it.copy(showSummonDialog = true, summonFiles = files, activeMaterialIndex = index) }
            }
        }
    }

    fun dismissSummonDialog() {
        _uiState.update { it.copy(showSummonDialog = false, summonFiles = emptyList(), activeMaterialIndex = -1) }
    }

    fun onReturnFromPlayback() {
        val wasPlayback = hasReturnedFromPlayback
        hasReturnedFromPlayback = false

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            refreshJournalDetail(isDeepCheck = false)
            _uiState.update { it.copy(isLoading = false) }

            // Check for auto-summon immediately on return
            val prefs = preferencesRepository.applicationPreferences.first()
            if (wasPlayback && prefs.autoPlayNextMaterial) {
                val state = _uiState.value
                val firstUnplayed = state.materials.firstOrNull { !it.isPlayed }
                if (firstUnplayed != null && !firstUnplayed.hasUserSelection) {
                    showSummonDialog(firstUnplayed.index)
                }
            }
        }
    }

    fun onPlaybackStarted() {
        hasReturnedFromPlayback = true
    }

    fun selectSummonFile(summonFile: SummonFile, onPlay: (Uri, String, Int) -> Unit) {
        viewModelScope.launch {
            val index = _uiState.value.activeMaterialIndex
            val material = _uiState.value.materials.getOrNull(index) ?: return@launch

            val prefs = preferencesRepository.applicationPreferences.first()
            val recursosUri = prefs.recursosUri ?: return@launch

            // Handle Lyric resolution for SOUNDTRACK
            var lyricPath: String? = null
            if (!material.lyricSummonPath.isNullOrEmpty()) {
                val baseName = summonFile.name.substringBeforeLast(".")
                val lyricFolder = Uri.parse(recursosUri).findFileByPath(context, material.lyricSummonPath)
                if (lyricFolder?.isDirectory == true) {
                    val lyricFile = lyricFolder.listFiles().find {
                        it.name?.substringBeforeLast(".")?.equals(baseName, ignoreCase = true) == true &&
                                (it.name?.lowercase()?.endsWith(".txt") == true || it.name?.lowercase()?.endsWith(".md") == true)
                    }
                    if (lyricFile != null) {
                        lyricPath = joinPaths(material.lyricSummonPath, lyricFile.name ?: "")
                    }
                }
            }

            // Register start time immediately (requirement d) - Add hyphen to indicate session started
            val startTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "-"

            dismissSummonDialog()

            // Start Playback IMMEDIATELY - Use the URI from summonFile directly
            summonFile.uri?.let { uri ->
                onPlaybackStarted()
                onPlay(uri, journalId, index)
            }

            // Perform persistence in background
            viewModelScope.launch(Dispatchers.IO) {
                journalSyncManager.updateMaterialSelection(
                    journalId = journalId,
                    materialIndex = index,
                    title = summonFile.name,
                    path = summonFile.path,
                    lyricPath = lyricPath,
                    datetimeRange = startTimestamp
                )
                // Refresh UI after save
                refreshJournalDetail(isDeepCheck = false)
            }
        }
    }

    fun showQuickView(summonFile: SummonFile) {
        viewModelScope.launch {
            val prefs = preferencesRepository.applicationPreferences.first()
            val recursosUriStr = prefs.recursosUri ?: return@launch
            val recursosUri = Uri.parse(recursosUriStr)

            // Requirement: If path contains "soundtracks", look for it in "lyrics" folder
            // e.g. "\2004\01. ___[Futari wa Precure]\01. soundtracks\Beautiful World.mp4"
            // -> "\2004\01. ___[Futari wa Precure]\01. lyrics\Beautiful World.md"
            val path = summonFile.path
            var content: String? = null

            if (path.contains("soundtracks", ignoreCase = true)) {
                val lastSoundtracksIndex = path.lastIndexOf("soundtracks", ignoreCase = true)
                val basePart = path.substring(0, lastSoundtracksIndex)
                val afterPart = path.substring(lastSoundtracksIndex + "soundtracks".length)

                // Reemplazar la carpeta soundtracks por lyrics
                val lyricsPathBase = basePart + "lyrics" + afterPart
                // Obtener el directorio de esa nueva ruta
                val lastSeparatorIndex = lyricsPathBase.lastIndexOfAny(charArrayOf('/', '\\'))
                val lyricsDir = if (lastSeparatorIndex != -1) lyricsPathBase.substring(0, lastSeparatorIndex) else ""

                val fileName = path.substringAfterLast("/", path.substringAfterLast("\\")).substringBeforeLast(".")

                Logger.logDebug("JournalDetailViewModel", "Searching lyrics for soundtrack in: $lyricsDir (original file: $fileName)")

                val lyricsFolder = recursosUri.findFileByPath(context, lyricsDir)
                if (lyricsFolder?.isDirectory == true) {
                    val lyricFile = lyricsFolder.listFiles().find {
                        it.name?.substringBeforeLast(".")?.equals(fileName, ignoreCase = true) == true &&
                                (it.name?.lowercase()?.endsWith(".md") == true || it.name?.lowercase()?.endsWith(".txt") == true)
                    }
                    if (lyricFile != null) {
                        Logger.logDebug("JournalDetailViewModel", "Found lyric file: ${lyricFile.name}")
                        content = withContext(Dispatchers.IO) {
                            try {
                                context.contentResolver.openInputStream(lyricFile.uri)?.use { it.bufferedReader().readText() }
                            } catch (e: Exception) {
                                null
                            }
                        }
                    } else {
                        Logger.logDebug("JournalDetailViewModel", "Lyric file not found for $fileName in $lyricsDir")
                    }
                } else {
                    Logger.logDebug("JournalDetailViewModel", "Lyrics directory not found: $lyricsDir")
                }
            }

            if (content == null) {
                Logger.logDebug("JournalDetailViewModel", "Falling back to standard sidecar resolution for $path")
                val videoUri = recursosUri.findFileByPath(context, path)?.uri
                if (videoUri != null) {
                    content = dev.anilbeesetti.nextplayer.core.media.TextSidecarResolver.resolve(context, videoUri)
                }
            }

            _uiState.update { it.copy(quickViewText = content) }
        }
    }

    fun dismissQuickView() {
        _uiState.update { it.copy(quickViewText = null) }
    }

    fun downloadMaterials() {
        verificationJob = viewModelScope.launch {
            try {
                // If nothing is missing, it behaves as "Verify existences"
                if (!_uiState.value.canDownload) {
                    _uiState.update { it.copy(isVerifying = true) }
                    refreshJournalDetail(isDeepCheck = true, reportProgress = true)
                    _uiState.update { it.copy(isVerifying = false) }
                }

                // If after refresh (or if it was already missing) there are materials to download, proceed
                if (!_uiState.value.canDownload) return@launch

                val prefs = preferencesRepository.applicationPreferences.first()
                val recursosUri = prefs.recursosUri ?: throw Exception("Recursos URI not configured")
                val port = prefs.serverPort
                var ip = prefs.manualServerIp ?: prefs.lastServerIp

                if (ip == null) {
                    ip = serverScanner.scan(port)
                }

                if (ip == null) throw Exception("Server not found")

                val materialsToDownload = _uiState.value.materials.filter {
                    !it.isDownloaded && (it.hasUserSelection || !it.summonPath.isNullOrEmpty() || !it.lyricSummonPath.isNullOrEmpty())
                }

                val pathsToDownload = mutableListOf<String>()
                for (material in materialsToDownload) {
                    if (!material.path.isNullOrEmpty()) {
                        pathsToDownload.add(material.path)
                        // If path is defined, we skip summon_path as they are mutually exclusive in execution
                        continue
                    }
                    if (!material.summonPath.isNullOrEmpty()) {
                        val downloadList = client.getDownloadList(ip, port, material.summonPath)
                        if (downloadList != null) {
                            for (fileInfo in downloadList.files) {
                                val localPath = joinPaths(downloadList.path, fileInfo.name)
                                if (!checkFileExists(recursosUri, localPath, fileInfo.size)) {
                                    pathsToDownload.add(localPath)
                                }
                            }
                        }
                    }
                    if (!material.lyricSummonPath.isNullOrEmpty()) {
                        val downloadList = client.getDownloadList(ip, port, material.lyricSummonPath)
                        if (downloadList != null) {
                            for (fileInfo in downloadList.files) {
                                if (fileInfo.name.lowercase().endsWith(".txt") || fileInfo.name.lowercase().endsWith(".md")) {
                                    val localPath = joinPaths(downloadList.path, fileInfo.name)
                                    if (!checkFileExists(recursosUri, localPath, fileInfo.size)) {
                                        pathsToDownload.add(localPath)
                                    }
                                }
                            }
                        }
                    }
                }

                if (pathsToDownload.isNotEmpty()) {
                    DownloadService.start(context, ip, port, recursosUri, pathsToDownload)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun stopDownloads() {
        verificationJob?.cancel()
        _uiState.update { it.copy(isVerifying = false) }
        DownloadService.stop(context)
    }

    private fun isVideo(path: String): Boolean {
        val extensions = listOf(".mp4", ".mkv", ".mov", ".webm")
        return extensions.any { path.lowercase().endsWith(it) }
    }
}
