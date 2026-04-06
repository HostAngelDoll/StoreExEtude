package dev.anilbeesetti.nextplayer.core.common

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

object ThumbnailGenerator {
    private const val TAG = "ThumbnailGenerator"

    suspend fun getThumbnail(context: Context, uri: Uri): Uri? = withContext(Dispatchers.IO) {
        val fileName = getHash(uri.toString()) + ".jpg"
        val thumbnailFile = File(context.cacheDir, fileName)

        if (thumbnailFile.exists()) {
            return@withContext Uri.fromFile(thumbnailFile)
        }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return@withContext null

            FileOutputStream(thumbnailFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
            }
            Uri.fromFile(thumbnailFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail for $uri", e)
            null
        } finally {
            retriever.release()
        }
    }

    private fun getHash(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
