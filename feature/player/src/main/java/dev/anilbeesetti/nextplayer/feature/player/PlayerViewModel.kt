package dev.anilbeesetti.nextplayer.feature.player

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.repository.MediaRepository
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.domain.GetSortedPlaylistUseCase
import dev.anilbeesetti.nextplayer.core.model.LoopMode
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.media.TextSidecarResolver
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import dev.anilbeesetti.nextplayer.core.model.Video
import dev.anilbeesetti.nextplayer.core.model.VideoContentScale
import dev.anilbeesetti.nextplayer.core.common.extensions.round
import dev.anilbeesetti.nextplayer.core.data.network.JournalSyncManager
import dev.anilbeesetti.nextplayer.core.data.network.SyncResponse
import dev.anilbeesetti.nextplayer.core.common.Logger
import dev.anilbeesetti.nextplayer.feature.player.state.SubtitleOptionsEvent
import dev.anilbeesetti.nextplayer.feature.player.state.VideoZoomEvent
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
    private val getSortedPlaylistUseCase: GetSortedPlaylistUseCase,
    private val journalSyncManager: JournalSyncManager,
) : ViewModel() {

    var playWhenReady: Boolean = true

    private val internalUiState = MutableStateFlow(
        PlayerUiState(
            playerPreferences = preferencesRepository.playerPreferences.value,
            applicationPreferences = preferencesRepository.applicationPreferences.value,
        ),
    )
    val uiState = internalUiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.playerPreferences.collect { prefs ->
                internalUiState.update { it.copy(playerPreferences = prefs) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.applicationPreferences.collect { prefs ->
                internalUiState.update { it.copy(applicationPreferences = prefs) }
            }
        }
    }

    fun updateSideTextPanelSplitRatio(ratio: Float) {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(sideTextPanelSplitRatio = ratio.coerceIn(0.25f, 0.75f))
            }
        }
    }

    fun loadVideoNotes(uri: Uri, context: android.content.Context) {
        viewModelScope.launch {
            val notes = TextSidecarResolver.resolve(context, uri)
            internalUiState.update { it.copy(videoNotes = notes) }
        }
    }

    suspend fun getPlaylistFromUri(uri: Uri): List<Video> {
        return getSortedPlaylistUseCase.invoke(uri)
    }

    fun updateVideoZoom(uri: String, zoom: Float) {
        viewModelScope.launch {
            mediaRepository.updateMediumZoom(uri, zoom)
        }
    }

    fun updatePlayerBrightness(value: Float) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(playerBrightness = value) }
        }
    }

    fun updateVideoContentScale(contentScale: VideoContentScale) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(playerVideoZoom = contentScale) }
        }
    }

    fun setLoopMode(loopMode: LoopMode) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(loopMode = loopMode) }
        }
    }

    fun onVideoZoomEvent(event: VideoZoomEvent) {
        when (event) {
            is VideoZoomEvent.ContentScaleChanged -> {
                updateVideoContentScale(event.contentScale)
            }
            is VideoZoomEvent.ZoomChanged -> {
                updateVideoZoom(event.mediaItem.mediaId, event.zoom)
            }
        }
    }

    fun onSubtitleOptionEvent(event: SubtitleOptionsEvent) {
        when (event) {
            is SubtitleOptionsEvent.DelayChanged -> {
                updateSubtitleDelay(event.mediaItem.mediaId, event.delay)
            }
            is SubtitleOptionsEvent.SpeedChanged -> {
                updateSubtitleSpeed(event.mediaItem.mediaId, event.speed)
            }
        }
    }

    private fun updateSubtitleDelay(uri: String, delay: Long) {
        viewModelScope.launch {
            mediaRepository.updateSubtitleDelay(uri, delay)
        }
    }

    private fun updateSubtitleSpeed(uri: String, speed: Float) {
        viewModelScope.launch {
            mediaRepository.updateSubtitleSpeed(uri, speed)
        }
    }

    fun toggleShowOSD() {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(showOSD = !it.showOSD)
            }
        }
    }

    fun toggleOsdShowDuration() {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(osdShowDuration = !it.osdShowDuration)
            }
        }
    }

    fun toggleOsdShowRemainingTime() {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(osdShowRemainingTime = !it.osdShowRemainingTime)
            }
        }
    }

    fun toggleOsdShowBattery() {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(osdShowBattery = !it.osdShowBattery)
            }
        }
    }

    fun toggleOsdShowClock() {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(osdShowClock = !it.osdShowClock)
            }
        }
    }

    fun updateOsdMarginPercent(value: Int) {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(osdMarginPercent = value)
            }
        }
    }

    fun toggleOsdShowBackground() {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(osdShowBackground = !it.osdShowBackground)
            }
        }
    }

    suspend fun getSyncData(): SyncResponse? {
        val prefs = preferencesRepository.applicationPreferences.value
        if (prefs.jornadasUri == null) {
            Logger.logDebug("PlayerViewModel", "jornadasUri is null in preferences.value")
        }
        return prefs.jornadasUri?.let { journalSyncManager.readSyncData(it) }
    }

    fun updateMaterialTracking(journalId: String, materialIndex: Int, datetimeRange: String) {
        viewModelScope.launch {
            journalSyncManager.updateMaterialTracking(journalId, materialIndex, datetimeRange)
        }
    }

    fun updateMaterialSelection(journalId: String, materialIndex: Int, title: String, path: String) {
        viewModelScope.launch {
            journalSyncManager.updateMaterialSelection(journalId, materialIndex, title, path)
        }
    }
}

@Stable
data class PlayerUiState(
    val playerPreferences: PlayerPreferences? = null,
    val applicationPreferences: ApplicationPreferences? = null,
    val videoNotes: String? = null,
)

sealed interface PlayerEvent
