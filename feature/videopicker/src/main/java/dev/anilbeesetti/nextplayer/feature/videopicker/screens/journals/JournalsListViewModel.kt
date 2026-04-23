package dev.anilbeesetti.nextplayer.feature.videopicker.screens.journals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.network.JournalSyncManager
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.Journal
import dev.anilbeesetti.nextplayer.core.ui.base.DataState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalsListViewModel @Inject constructor(
    private val journalSyncManager: JournalSyncManager,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        preferencesRepository.applicationPreferences,
        refreshTrigger
    ) { prefs, _ -> prefs.jornadasUri }
        .flatMapLatest { jornadasUri ->
            flow {
                emit(DataState.Loading)
                if (jornadasUri == null) {
                    emit(DataState.Error(Exception("No journals folder configured")))
                } else {
                    val journals = journalSyncManager.readJournals(jornadasUri)
                    emit(DataState.Success(journals))
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DataState.Loading,
        )

    fun refresh() {
        refreshTrigger.update { it + 1 }
    }

    fun syncJournals() {
        viewModelScope.launch {
            val result = journalSyncManager.sync()
            journalSyncManager.showSyncResultMessage(result)
            refresh()
        }
    }
}
