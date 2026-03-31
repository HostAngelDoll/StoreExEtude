package dev.anilbeesetti.nextplayer.core.domain

import dev.anilbeesetti.nextplayer.core.common.Dispatcher
import dev.anilbeesetti.nextplayer.core.common.NextDispatchers
import dev.anilbeesetti.nextplayer.core.data.repository.MediaRepository
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import android.net.Uri
import dev.anilbeesetti.nextplayer.core.model.Sort
import dev.anilbeesetti.nextplayer.core.model.Video
import java.io.File
import androidx.documentfile.provider.DocumentFile
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

class GetSortedVideosUseCase @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context,
    @Dispatcher(NextDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    operator fun invoke(folderPath: String? = null): Flow<List<Video>> {
        val videosFlow = if (folderPath != null) {
            mediaRepository.getVideosFlowFromFolderPath(folderPath)
        } else {
            mediaRepository.getVideosFlow()
        }

        return combine(
            videosFlow,
            preferencesRepository.applicationPreferences,
        ) { videoItems, preferences ->

            val nonExcludedVideos = videoItems.filterNot {
                it.parentPath in preferences.excludeFolders
            }

            val sort = Sort(by = preferences.sortBy, order = preferences.sortOrder)
            val sorted = nonExcludedVideos.sortedWith(sort.videoComparator())

            sorted.map { video ->
                val extensions = listOf("txt", "md")

                // Try DocumentFile for content:// URIs
                val uri = runCatching { Uri.parse(video.uriString) }.getOrNull()
                val doc = uri?.let { DocumentFile.fromSingleUri(context, it) }
                val parent = doc?.parentFile
                val baseName = doc?.name?.substringBeforeLast(".")

                var notesExtension: String? = null
                if (parent != null && baseName != null) {
                    notesExtension = extensions.firstOrNull { ext ->
                        parent.findFile("$baseName.$ext")?.exists() == true
                    }
                }

                // Fallback to File API for file paths
                if (notesExtension == null) {
                    val videoFile = File(video.path)
                    val videoNameWithoutExtension = videoFile.nameWithoutExtension
                    notesExtension = extensions.firstOrNull { ext ->
                        File(videoFile.parent, "$videoNameWithoutExtension.$ext").exists()
                    }
                }

                video.copy(notesExtension = notesExtension)
            }
        }.flowOn(defaultDispatcher)
    }
}
