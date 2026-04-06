package dev.anilbeesetti.nextplayer.feature.videopicker.navigation

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import dev.anilbeesetti.nextplayer.feature.videopicker.screens.journals.JournalDetailRoute
import dev.anilbeesetti.nextplayer.feature.videopicker.screens.journals.JournalsListRoute
import dev.anilbeesetti.nextplayer.feature.videopicker.screens.mediapicker.MediaPickerRoute
import kotlinx.serialization.Serializable

internal const val folderIdArg = "folderId"

internal class FolderArgs(val folderId: String?) {
    constructor(savedStateHandle: SavedStateHandle) :
        this(savedStateHandle.get<String>(folderIdArg)?.let { Uri.decode(it) })
}

@Serializable
data class MediaPickerRoute(
    val folderId: String? = null,
)

@Serializable
object JournalsListRoute

@Serializable
data class JournalDetailRoute(
    val journalId: String,
)

fun NavController.navigateToMediaPickerScreen(
    folderId: String,
    navOptions: NavOptions? = null,
) {
    val encodedFolderId = Uri.encode(folderId)
    this.navigate(MediaPickerRoute(encodedFolderId), navOptions)
}

fun NavController.navigateToJournalsListScreen(navOptions: NavOptions? = null) {
    this.navigate(JournalsListRoute, navOptions)
}

fun NavController.navigateToJournalDetailScreen(
    journalId: String,
    navOptions: NavOptions? = null,
) {
    this.navigate(JournalDetailRoute(journalId), navOptions)
}

fun NavGraphBuilder.mediaPickerScreen(
    onNavigateUp: () -> Unit,
    onPlayVideo: (uri: Uri) -> Unit,
    onPlayVideos: (uris: List<Uri>) -> Unit,
    onFolderClick: (folderPath: String) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onManageJournalsClick: () -> Unit,
    onJournalDetailClick: (String) -> Unit,
) {
    composable<MediaPickerRoute> {
        MediaPickerRoute(
            onPlayVideo = onPlayVideo,
            onPlayVideos = onPlayVideos,
            onNavigateUp = onNavigateUp,
            onFolderClick = onFolderClick,
            onSettingsClick = onSettingsClick,
            onSearchClick = onSearchClick,
            onManageJournalsClick = onManageJournalsClick,
        )
    }
    composable<JournalsListRoute> {
        JournalsListRoute(
            onSettingsClick = onSettingsClick,
            onNavigateUp = onNavigateUp,
            onJournalClick = { journalId ->
                onJournalDetailClick(journalId)
            },
        )
    }
    composable<JournalDetailRoute> {
        JournalDetailRoute(
            onNavigateUp = onNavigateUp,
            onPlayVideo = onPlayVideo,
        )
    }
}
