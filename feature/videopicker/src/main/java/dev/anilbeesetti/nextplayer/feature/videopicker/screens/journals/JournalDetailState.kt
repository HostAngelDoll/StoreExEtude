package dev.anilbeesetti.nextplayer.feature.videopicker.screens.journals

import android.net.Uri

data class MaterialUiModel(
    val index: Int,
    val title: String,
    val originalFileName: String?,
    val path: String?,
    val summonPath: String?,
    val lyricSummonPath: String? = null,
    val isDownloaded: Boolean,
    val duration: String?,
    val hasSidecar: Boolean,
    val hasUserSelection: Boolean,
    val isPlayed: Boolean,
    val isStarted: Boolean = false,
    val uri: Uri?,
    val thumbnailUri: Uri? = null,
    val missingFilesCount: Int = 0,
)

data class JournalDetailUiState(
    val journalId: String = "",
    val name: String = "",
    val expectedDate: Long = 0,
    val status: String = "",
    val updatedAt: Long = 0,
    val materials: List<MaterialUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isDownloading: Boolean = false,
    val isVerifying: Boolean = false,
    val currentFileName: String? = null,
    val fileProgress: Float = 0f,
    val overallProgress: Float = 0f,
    val error: String? = null,
    val showSummonDialog: Boolean = false,
    val summonFiles: List<SummonFile> = emptyList(),
    val activeMaterialIndex: Int = -1,
    val quickViewText: String? = null,
) {
    val canDownload: Boolean
        get() = materials.any { !it.isDownloaded && (it.hasUserSelection || !it.summonPath.isNullOrEmpty() || !it.lyricSummonPath.isNullOrEmpty()) }

    val canExecute: Boolean
        get() = materials.all { it.isDownloaded || !it.hasUserSelection } && materials.any { it.isDownloaded && !it.isPlayed }

    val hasProgress: Boolean
        get() = materials.any { it.isPlayed || it.isStarted }

    val canReset: Boolean
        get() = materials.any { it.isPlayed || !it.hasUserSelection }

    val canUpload: Boolean
        get() = materials.all { it.isPlayed && it.hasUserSelection }
}

data class SummonFile(
    val name: String,
    val uri: Uri? = null,
    val path: String,
    val isWatched: Boolean,
    val sidecarText: String? = null,
)
