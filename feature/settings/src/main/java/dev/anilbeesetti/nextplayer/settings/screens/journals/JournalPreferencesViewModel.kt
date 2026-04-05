package dev.anilbeesetti.nextplayer.settings.screens.journals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.common.Utils
import dev.anilbeesetti.nextplayer.core.data.network.JournalSyncManager
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalPreferencesViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val journalSyncManager: JournalSyncManager,
) : ViewModel() {

    val uiState = preferencesRepository.applicationPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ApplicationPreferences(),
    )

    fun updateSyncOnAppStart(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences { it.copy(syncOnAppStart = enabled) }
        }
    }

    fun updateRecursosUri(uri: String?) {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences { it.copy(recursosUri = uri) }
        }
    }

    fun updateJornadasUri(uri: String?) {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences { it.copy(jornadasUri = uri) }
        }
    }

    fun updateManualServerIp(ip: String?) {
        viewModelScope.launch {
            val cleanIp = ip?.let { Utils.cleanIpAddress(it) }
            preferencesRepository.updateApplicationPreferences { it.copy(manualServerIp = cleanIp) }
        }
    }

    fun updateServerPort(port: Int) {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences { it.copy(serverPort = port) }
        }
    }

    fun sync() {
        viewModelScope.launch {
            val result = journalSyncManager.sync()
            journalSyncManager.showSyncResultMessage(result)
        }
    }
}
