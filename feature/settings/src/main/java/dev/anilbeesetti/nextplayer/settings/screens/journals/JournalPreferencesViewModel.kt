package dev.anilbeesetti.nextplayer.settings.screens.journals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalPreferencesViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
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
            preferencesRepository.updateApplicationPreferences { it.copy(manualServerIp = ip) }
        }
    }

    fun updateServerPort(port: Int) {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences { it.copy(serverPort = port) }
        }
    }
}
