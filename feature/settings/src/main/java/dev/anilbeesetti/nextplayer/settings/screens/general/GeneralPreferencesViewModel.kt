package dev.anilbeesetti.nextplayer.settings.screens.general

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.common.Logger
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.media.sync.MediaInfoSynchronizer
import dev.anilbeesetti.nextplayer.core.model.SettingsBundle
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@HiltViewModel
class GeneralPreferencesViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val mediaInfoSynchronizer: MediaInfoSynchronizer,
) : ViewModel() {

    private val uiStateInternal = MutableStateFlow(GeneralPreferencesUiState())
    val uiState = uiStateInternal.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun onEvent(event: GeneralPreferencesUiEvent) {
        when (event) {
            is GeneralPreferencesUiEvent.ShowDialog -> showDialog(event.value)
            GeneralPreferencesUiEvent.ClearThumbnailCache -> clearThumbnailCache()
            GeneralPreferencesUiEvent.ResetSettings -> resetSettings()
            is GeneralPreferencesUiEvent.ExportSettings -> exportSettings(event.contentResolver, event.uri)
            is GeneralPreferencesUiEvent.ImportSettings -> importSettings(event.contentResolver, event.uri)
            GeneralPreferencesUiEvent.DismissError -> dismissError()
        }
    }

    private fun showDialog(value: GeneralPreferencesDialog?) {
        uiStateInternal.value = uiStateInternal.value.copy(showDialog = value)
    }

    private fun clearThumbnailCache() {
        viewModelScope.launch {
            mediaInfoSynchronizer.clearThumbnailsCache()
        }
    }

    private fun resetSettings() {
        viewModelScope.launch {
            preferencesRepository.resetPreferences()
        }
    }

    private fun exportSettings(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val bundle = preferencesRepository.getSettingsBundle()
                    val jsonString = json.encodeToString(SettingsBundle.serializer(), bundle)
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray())
                    }
                } catch (e: Exception) {
                    Logger.logError("GeneralPreferencesViewModel", "Failed to export settings: $e")
                }
            }
        }
    }

    private fun importSettings(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.readBytes().decodeToString()
                    } ?: throw Exception("Failed to read file")

                    val bundle = json.decodeFromString(SettingsBundle.serializer(), jsonString)
                    if (bundle.version > 1) {
                        throw Exception("Unsupported settings version: ${bundle.version}")
                    }
                    preferencesRepository.importSettingsBundle(bundle)
                } catch (e: Exception) {
                    Logger.logError("GeneralPreferencesViewModel", "Failed to import settings: $e")
                    withContext(Dispatchers.Main) {
                        uiStateInternal.value = uiStateInternal.value.copy(showError = true)
                    }
                }
            }
        }
    }

    private fun dismissError() {
        uiStateInternal.value = uiStateInternal.value.copy(showError = false)
    }
}

data class GeneralPreferencesUiState(
    val showDialog: GeneralPreferencesDialog? = null,
    val showError: Boolean = false,
)

sealed interface GeneralPreferencesDialog {
    data object ClearThumbnailCacheDialog : GeneralPreferencesDialog
    data object ResetSettingsDialog : GeneralPreferencesDialog
    data class ImportSettingsConfirmationDialog(val uri: Uri) : GeneralPreferencesDialog
}

sealed interface GeneralPreferencesUiEvent {
    data class ShowDialog(val value: GeneralPreferencesDialog?) : GeneralPreferencesUiEvent
    data object ClearThumbnailCache : GeneralPreferencesUiEvent
    data object ResetSettings : GeneralPreferencesUiEvent
    data class ExportSettings(val contentResolver: ContentResolver, val uri: Uri) : GeneralPreferencesUiEvent
    data class ImportSettings(val contentResolver: ContentResolver, val uri: Uri) : GeneralPreferencesUiEvent
    data object DismissError : GeneralPreferencesUiEvent
}
