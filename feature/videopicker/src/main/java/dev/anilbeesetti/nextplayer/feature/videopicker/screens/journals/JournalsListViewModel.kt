package dev.anilbeesetti.nextplayer.feature.videopicker.screens.journals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.model.Journal
import dev.anilbeesetti.nextplayer.core.ui.base.DataState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalsListViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<DataState<List<Journal>>>(DataState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadJournals()
    }

    fun syncJournals() {
        viewModelScope.launch {
            _uiState.value = DataState.Loading
            delay(1500) // Simulate sync
            _uiState.value = DataState.Success(Journal.samples)
        }
    }

    private fun loadJournals() {
        viewModelScope.launch {
            delay(500) // Simulate loading
            _uiState.value = DataState.Success(Journal.samples)
        }
    }
}
