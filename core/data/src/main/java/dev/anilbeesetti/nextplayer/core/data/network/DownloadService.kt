package dev.anilbeesetti.nextplayer.core.data.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.AndroidEntryPoint
import dev.anilbeesetti.nextplayer.core.common.extensions.getOrCreateFileByPath
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadProgress(
    val currentFileName: String? = null,
    val fileProgress: Float = 0f,
    val overallProgress: Float = 0f,
    val isDownloading: Boolean = false
)

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var client: StoreEtudeClient

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var downloadJob: Job? = null
    private var lastNotificationTime = 0L

    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress.asStateFlow()

    companion object {
        private const val CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID = 101
        private const val ACTION_START = "ACTION_START"
        private const val ACTION_STOP = "ACTION_STOP"

        fun start(context: Context, ip: String, port: Int, resourcesUri: String, paths: List<String>) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
                putExtra("ip", ip)
                putExtra("port", port)
                putExtra("resourcesUri", resourcesUri)
                putStringArrayListExtra("paths", ArrayList(paths))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val ip = intent.getStringExtra("ip") ?: ""
                val port = intent.getIntExtra("port", 8080)
                val resourcesUri = intent.getStringExtra("resourcesUri") ?: ""
                val paths = intent.getStringArrayListExtra("paths") ?: emptyList<String>()
                startDownload(ip, port, resourcesUri, paths)
            }
            ACTION_STOP -> {
                stopDownload()
            }
        }
        return START_NOT_STICKY
    }

    private fun startDownload(ip: String, port: Int, resourcesUri: String, paths: List<String>) {
        downloadJob?.cancel()
        _progress.update { it.copy(isDownloading = true, overallProgress = 0f) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification("Iniciando descarga...", 0), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, createNotification("Iniciando descarga...", 0))
        }

        downloadJob = serviceScope.launch {
            try {
                paths.forEachIndexed { index, path ->
                    _progress.update { it.copy(currentFileName = path.substringAfterLast('/'), fileProgress = 0f) }
                    updateNotification(_progress.value.currentFileName ?: "", (_progress.value.overallProgress * 100).toInt())

                    downloadSingleFile(ip, port, path, resourcesUri) { fileBytesRead, totalBytes ->
                        val fileProgress = if (totalBytes > 0) fileBytesRead.toFloat() / totalBytes else 0f
                        val overallProgress = (index + fileProgress) / paths.size
                        _progress.update { it.copy(fileProgress = fileProgress, overallProgress = overallProgress) }
                        updateNotification(_progress.value.currentFileName ?: "", (overallProgress * 100).toInt())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _progress.update { it.copy(isDownloading = false, currentFileName = null, fileProgress = 0f) }
                stopForeground(true)
                stopSelf()
            }
        }
    }

    private suspend fun downloadSingleFile(ip: String, port: Int, path: String, resourcesUri: String, onProgress: (Long, Long) -> Unit) {
        val treeUri = Uri.parse(resourcesUri)
        val file = treeUri.getOrCreateFileByPath(this, path) ?: return

        try {
            client.downloadFileWithProgress(ip, port, path, onProgress).execute { response ->
                contentResolver.openOutputStream(file.uri)?.use { output ->
                    response.bodyAsChannel().toInputStream().copyTo(output)
                }
            }
        } catch (e: Exception) {
            file.delete() // Remove partial file on failure/cancellation
            throw e
        }
    }

    private fun stopDownload() {
        downloadJob?.cancel()
        _progress.update { it.copy(isDownloading = false, currentFileName = null, fileProgress = 0f) }
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Descargas de recursos", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String, progress: Int) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Descargando recursos")
        .setContentText(content)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setProgress(100, progress, false)
        .setOngoing(true)
        .build()

    private fun updateNotification(content: String, progress: Int) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNotificationTime >= 500) {
            val notification = createNotification(content, progress)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
            lastNotificationTime = currentTime
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class LocalBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }
}
