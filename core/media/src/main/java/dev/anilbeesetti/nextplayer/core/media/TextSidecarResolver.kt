package dev.anilbeesetti.nextplayer.core.media

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dev.anilbeesetti.nextplayer.core.common.extensions.getPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object TextSidecarResolver {

    suspend fun resolve(context: Context, videoUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val extensions = listOf("txt", "md")

                // Try with DocumentFile for content:// or file://
                val doc = DocumentFile.fromSingleUri(context, videoUri)
                val parent = doc?.parentFile
                val baseName = doc?.name?.substringBeforeLast(".")

                if (parent != null && baseName != null) {
                    for (ext in extensions) {
                        val notesFile = parent.findFile("$baseName.$ext")
                        if (notesFile?.exists() == true && notesFile.isFile) {
                            return@withContext context.contentResolver.openInputStream(notesFile.uri)
                                ?.use { it.bufferedReader().readText() }
                        }
                    }
                }

                // Fallback to absolute path for file:// or MediaStore where DocumentFile parent might be null
                val path = if (videoUri.scheme == "file") {
                    videoUri.path
                } else {
                    context.getPath(videoUri)
                }

                if (path != null) {
                    val videoFile = File(path)
                    val videoNameWithoutExtension = videoFile.nameWithoutExtension
                    for (ext in extensions) {
                        val notesFile = File(videoFile.parent, "$videoNameWithoutExtension.$ext")
                        if (notesFile.exists() && notesFile.isFile) {
                            return@withContext notesFile.readText()
                        }
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }
    }
}
