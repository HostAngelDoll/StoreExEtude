package dev.anilbeesetti.nextplayer.core.media

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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

        // Strategy 1: Direct File access (if scheme is file or we have a valid path)
        val path = when (videoUri.scheme) {
            ContentResolver.SCHEME_FILE -> videoUri.path
            else -> context.getPath(videoUri)
        }
        if (path != null) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                val resolved = resolveFromDirectory(file.parentFile, baseName)
                if (resolved != null) return resolved
            }
        }

        // Strategies for content:// URIs
        if (videoUri.scheme == ContentResolver.SCHEME_CONTENT) {
            // Strategy 2: MediaStore Directory Query
            val resolvedDir = resolveFromMediaStore(context, videoUri, baseName)
            if (resolvedDir != null) return resolvedDir

            // Strategy 3: MediaStore Smart Lookup (Targeted by name)
            val resolvedSmart = resolveFromMediaStoreSmartLookup(context, videoUri, baseName)
            if (resolvedSmart != null) return resolvedSmart

            // Strategy 4: SAF (DocumentFile)
            val resolvedDoc = resolveFromDocumentFile(context, videoUri, baseName)
            if (resolvedDoc != null) return resolvedDoc
        }

        // Strategy 5: User Configured SAF Folders
        val resolvedFromSaf = resolveFromUserConfiguredFolders(context, baseName)
        if (resolvedFromSaf != null) return resolvedFromSaf

        return null
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

    private fun resolveFromMediaStore(context: Context, videoUri: Uri, baseName: String): String? {
        val (relativePath, data) = getMediaStorePaths(context, videoUri)

        val externalFilesUri = MediaStore.Files.getContentUri("external")
        val filesProjection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME)

        val selection: String
        val selectionArgs: Array<String>

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val path = relativePath ?: return null
            selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} = ? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            selectionArgs = arrayOf(path, "$baseName.%")
        } else {
            val parentPath = data?.let { File(it).parent } ?: return null
            // For older versions, we match by parent path in DATA column
            selection = "${MediaStore.Files.FileColumns.DATA} LIKE ? AND ${MediaStore.Files.FileColumns.DATA} NOT LIKE ? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            selectionArgs = arrayOf("$parentPath/%", "$parentPath/%/%", "$baseName.%")
        }

        context.contentResolver.query(externalFilesUri, filesProjection, selection, selectionArgs, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val fileName = cursor.getString(nameColumn)
                if (sidecarExtensions.any { ext -> fileName.equals("$baseName.$ext", ignoreCase = true) }) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = MediaStore.Files.getContentUri("external", id)
                    return readText(context, contentUri)
                }
            }
        }

        return null
    }

    /**
     * Targeted exact name query for sidecars.
     * This is more reliable than broad directory listing in some MediaStore implementations.
     */
    private fun resolveFromMediaStoreSmartLookup(context: Context, videoUri: Uri, baseName: String): String? {
        val (relativePath, data) = getMediaStorePaths(context, videoUri)
        if (relativePath == null && data == null) return null

        val externalFilesUri = MediaStore.Files.getContentUri("external")
        val filesProjection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME)

        for (ext in sidecarExtensions) {
            val targetName = "$baseName.$ext"

            val selection: String
            val selectionArgs: Array<String>

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (relativePath == null) continue
                selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} = ? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?"
                selectionArgs = arrayOf(relativePath, targetName)
            } else {
                val parentPath = data?.let { File(it).parent } ?: continue
                val targetPath = "$parentPath/$targetName"
                selection = "${MediaStore.Files.FileColumns.DATA} = ?"
                selectionArgs = arrayOf(targetPath)
            }

            context.contentResolver.query(externalFilesUri, filesProjection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val id = cursor.getLong(idColumn)
                    val contentUri = MediaStore.Files.getContentUri("external", id)
                    return readText(context, contentUri)
                }
            }
        }

        return null
    }

    private fun getMediaStorePaths(context: Context, videoUri: Uri): Pair<String?, String?> {
        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(MediaStore.Video.Media.RELATIVE_PATH, MediaStore.Video.Media.DISPLAY_NAME)
        } else {
            arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME)
        }

        var relativePath: String? = null
        var data: String? = null

        context.contentResolver.query(videoUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val index = cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                    if (index != -1) relativePath = cursor.getString(index)
                } else {
                    val index = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                    if (index != -1) data = cursor.getString(index)
                }
            }
        }
        return Pair(relativePath, data)
    }

    private fun resolveFromDocumentFile(context: Context, videoUri: Uri, baseName: String): String? {
        val document = DocumentFile.fromSingleUri(context, videoUri) ?: return null
        val parent = document.parentFile ?: return null

        for (ext in sidecarExtensions) {
            val lower = parent.findFile("$baseName.$ext")
            if (lower?.isFile == true) return readText(context, lower.uri)

            val upper = parent.findFile("$baseName.${ext.uppercase()}")
            if (upper?.isFile == true) return readText(context, upper.uri)
        }

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

    private fun resolveFromUserConfiguredFolders(context: Context, baseName: String): String? {
        // Hook for future implementation of user-configured SAF folders
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
