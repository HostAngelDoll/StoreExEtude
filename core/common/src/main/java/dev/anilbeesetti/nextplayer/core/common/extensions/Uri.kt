package dev.anilbeesetti.nextplayer.core.common.extensions

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * Whether the Uri authority is ExternalStorageProvider.
 */
val Uri.isExternalStorageDocument: Boolean
    get() = "com.android.externalstorage.documents" == authority

/**
 * Whether the Uri authority is DownloadsProvider.
 */
val Uri.isDownloadsDocument: Boolean
    get() = "com.android.providers.downloads.documents" == authority

/**
 * Whether the Uri authority is MediaProvider.
 */
val Uri.isMediaDocument: Boolean
    get() = "com.android.providers.media.documents" == authority

/**
 * Whether the Uri authority is Google Photos.
 */
val Uri.isGooglePhotosUri: Boolean
    get() = "com.google.android.apps.photos.content" == authority

/**
 * Whether the Uri authority is PhotoPicker.
 */
val Uri.isLocalPhotoPickerUri: Boolean
    get() = toString().contains("com.android.providers.media.photopicker")

/**
 * Whether the Uri authority is PhotoPicker.
 */
val Uri.isCloudPhotoPickerUri: Boolean
    get() = toString().contains("com.google.android.apps.photos.cloudpicker")

fun Uri.findFileByPath(context: Context, path: String): DocumentFile? {
    var currentFile = DocumentFile.fromTreeUri(context, this) ?: return null
    val segments = path.split("/").filter { it.isNotEmpty() }
    for (segment in segments) {
        currentFile = currentFile.findFile(segment) ?: return null
    }
    return currentFile
}

fun Uri.getOrCreateFileByPath(context: Context, path: String, mimeType: String = "application/octet-stream"): DocumentFile? {
    var currentFile = DocumentFile.fromTreeUri(context, this) ?: return null
    val segments = path.split("/").filter { it.isNotEmpty() }
    for (i in 0 until segments.size - 1) {
        val segment = segments[i]
        currentFile = currentFile.findFile(segment) ?: currentFile.createDirectory(segment) ?: return null
    }
    val fileName = segments.last()
    return currentFile.findFile(fileName) ?: currentFile.createFile(mimeType, fileName)
}
