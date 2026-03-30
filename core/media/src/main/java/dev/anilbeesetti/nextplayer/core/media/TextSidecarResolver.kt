package dev.anilbeesetti.nextplayer.core.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import dev.anilbeesetti.nextplayer.core.common.extensions.getPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object TextSidecarResolver {
    private val sidecarExtensions = listOf("txt", "md")

    suspend fun resolve(context: Context, videoUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                when (videoUri.scheme) {
                    "file" -> resolveFromFilePath(videoUri.path)
                    "content" -> resolveFromContentUri(context, videoUri)
                    else -> resolveFromFallbackPath(context, videoUri)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun resolveFromContentUri(context: Context, videoUri: Uri): String? {
        val documentFile = DocumentFile.fromSingleUri(context, videoUri)
        val displayName = queryDisplayName(context, videoUri)
            ?: documentFile?.name

        val baseName = displayName
            ?.substringBeforeLast('.', displayName)
            ?.takeIf { it.isNotBlank() }

        val parent = documentFile?.parentFile
        if (parent != null && baseName != null) {
            for (ext in sidecarExtensions) {
                val candidate = parent.findFile("$baseName.$ext")
                if (candidate?.exists() == true && candidate.isFile) {
                    readText(context, candidate.uri)?.let { return it }
                }
            }
        }

        return resolveFromFallbackPath(context, videoUri)
    }

    private fun resolveFromFallbackPath(context: Context, videoUri: Uri): String? {
        val path = when (videoUri.scheme) {
            "file" -> videoUri.path
            else -> context.getPath(videoUri)
        } ?: return null

        return resolveFromFilePath(path)
    }

    private fun resolveFromFilePath(path: String?): String? {
        val videoPath = path ?: return null
        return resolveFromFile(File(videoPath))
    }

    private fun resolveFromFile(videoFile: File): String? {
        val parent = videoFile.parentFile ?: return null
        val baseName = videoFile.nameWithoutExtension

        for (ext in sidecarExtensions) {
            val candidate = File(parent, "$baseName.$ext")
            if (candidate.isFile) {
                return candidate.readText()
            }
        }

        return null
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun readText(context: Context, uri: Uri): String? {
        return context.contentResolver.openInputStream(uri)
            ?.bufferedReader()
            ?.use { it.readText() }
    }
}
