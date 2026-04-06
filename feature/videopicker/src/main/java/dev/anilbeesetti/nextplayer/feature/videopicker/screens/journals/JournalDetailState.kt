package dev.anilbeesetti.nextplayer.feature.videopicker.screens.journals

import android.net.Uri

data class MaterialUiModel(
    val index: Int,
    val title: String,
    val originalFileName: String?,
    val path: String?,
    val isDownloaded: Boolean,
    val duration: String?,
    val hasSidecar: Boolean,
    val hasUserSelection: Boolean,
    val isPlayed: Boolean,
    val uri: Uri?,
)

data class JournalDetailUiState(
    val journalId: String = "",
    val name: String = "",
    val expectedDate: Long = 0,
    val status: String = "",
    val updatedAt: Long = 0,
    val materials: List<MaterialUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val canDownload: Boolean
        get() = materials.any { !it.isDownloaded && it.hasUserSelection }

    val canExecute: Boolean
        get() = materials.all { it.isDownloaded || !it.hasUserSelection } && materials.any { it.isDownloaded }

    val canReset: Boolean
        get() = materials.any { it.isPlayed || !it.hasUserSelection }

    val canUpload: Boolean
        get() = materials.all { it.isPlayed && it.hasUserSelection }
}
