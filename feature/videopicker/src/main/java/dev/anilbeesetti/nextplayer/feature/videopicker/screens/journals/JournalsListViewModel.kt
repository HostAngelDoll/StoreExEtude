package dev.anilbeesetti.nextplayer.feature.videopicker.screens.journals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.network.JournalSyncManager
import dev.anilbeesetti.nextplayer.core.database.dao.JournalDao
import dev.anilbeesetti.nextplayer.core.database.entities.asExternalModel
import dev.anilbeesetti.nextplayer.core.model.Journal
import dev.anilbeesetti.nextplayer.core.ui.base.DataState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalsListViewModel @Inject constructor(
    private val journalSyncManager: JournalSyncManager,
    private val journalDao: JournalDao,
) : ViewModel() {

    val uiState = journalDao.getActiveJournals().map { entities ->
        DataState.Success(entities.map { it.asExternalModel() })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DataState.Loading,
    )

    fun syncJournals() {
        viewModelScope.launch {
            val result = journalSyncManager.sync()
            journalSyncManager.showSyncResultMessage(result)
        }
    }
}
