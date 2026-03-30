package dev.anilbeesetti.nextplayer.core.media

import android.content.ContentResolver
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
            runCatching {
                resolveInternal(context, videoUri)
            }.getOrNull()
        }
    }

    private fun resolveInternal(context: Context, videoUri: Uri): String? {
        val baseName = resolveBaseName(context, videoUri) ?: return null

        return when (videoUri.scheme) {
            ContentResolver.SCHEME_FILE -> {
                val path = videoUri.path ?: return null
                resolveFromDirectory(File(path).parentFile, baseName)
            }

            ContentResolver.SCHEME_CONTENT -> {
                resolveFromDocumentFile(context, videoUri, baseName)
                    ?: resolveFromDirectory(context.getPath(videoUri)?.let(::File)?.parentFile, baseName)
            }

            else -> {
                resolveFromDirectory(context.getPath(videoUri)?.let(::File)?.parentFile, baseName)
            }
        }
    }

    private fun resolveBaseName(context: Context, videoUri: Uri): String? {
        val fromDocument = DocumentFile.fromSingleUri(context, videoUri)?.name?.toBaseName()
        if (!fromDocument.isNullOrBlank()) return fromDocument

        val fromQuery = context.contentResolver.query(
            videoUri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                cursor.getString(index)
            } else {
                null
            }
        }?.toBaseName()

        if (!fromQuery.isNullOrBlank()) return fromQuery

        val path = when (videoUri.scheme) {
            ContentResolver.SCHEME_FILE -> videoUri.path
            else -> context.getPath(videoUri)
        }

        return path?.let { File(it).nameWithoutExtension }.takeUnless { it.isNullOrBlank() }
    }

    private fun resolveFromDocumentFile(context: Context, videoUri: Uri, baseName: String): String? {
        val document = DocumentFile.fromSingleUri(context, videoUri) ?: return null
        val parent = document.parentFile ?: return null

        for (ext in sidecarExtensions) {
            for (candidate in listOf("$baseName.$ext", "$baseName.${ext.uppercase()}")) {
                val sidecar = parent.findFile(candidate)
                if (sidecar?.isFile == true) {
                    return readText(context, sidecar.uri)
                }
            }
        }

        parent.listFiles()
            .firstOrNull { file ->
                file.isFile && sidecarExtensions.any { ext ->
                    file.name.equals("$baseName.$ext", ignoreCase = true)
                }
            }
            ?.let { return readText(context, it.uri) }

        return null
    }

    private fun resolveFromDirectory(directory: File?, baseName: String): String? {
        if (directory == null || !directory.exists() || !directory.isDirectory) return null

        for (ext in sidecarExtensions) {
            val lower = File(directory, "$baseName.$ext")
            if (lower.isFile) return lower.readText()

            val upper = File(directory, "$baseName.${ext.uppercase()}")
            if (upper.isFile) return upper.readText()
        }

        return null
    }

    private fun readText(context: Context, uri: Uri): String? {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().readText()
        }
    }

    private fun String.toBaseName(): String {
        return substringBeforeLast('.', this).ifBlank { this }
    }
}
