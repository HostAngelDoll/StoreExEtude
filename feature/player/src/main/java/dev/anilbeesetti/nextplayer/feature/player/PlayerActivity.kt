package dev.anilbeesetti.nextplayer.feature.player

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import dev.anilbeesetti.nextplayer.core.model.ScreenOrientation
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.util.Consumer
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import dev.anilbeesetti.nextplayer.core.common.Logger
import dev.anilbeesetti.nextplayer.core.common.extensions.getMediaContentUri
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.feature.player.extensions.registerForSuspendActivityResult
import dev.anilbeesetti.nextplayer.core.ui.components.NextDialog
import dev.anilbeesetti.nextplayer.feature.player.extensions.setExtras
import dev.anilbeesetti.nextplayer.feature.player.extensions.uriToSubtitleConfiguration
import dev.anilbeesetti.nextplayer.feature.player.service.PlayerService
import dev.anilbeesetti.nextplayer.feature.player.service.addSubtitleTrack
import dev.anilbeesetti.nextplayer.feature.player.service.stopPlayerSession
import dev.anilbeesetti.nextplayer.feature.player.utils.PlayerApi
import dev.anilbeesetti.nextplayer.core.common.extensions.findFileByPath
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val LocalUseMaterialYouControls = compositionLocalOf { false }

@SuppressLint("UnsafeOptInUsageError")
@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()
    val playerPreferences get() = viewModel.uiState.value.playerPreferences

    private val onWindowAttributesChangedListener = CopyOnWriteArrayList<Consumer<WindowManager.LayoutParams?>>()

    private var isPlaybackFinished = false
    private var playInBackground: Boolean = false
    private var isIntentNew: Boolean = true

    private var journalId: String? = null
    private var materialIndex: Int = -1
    private var pendingMaterialIndex: Int = -1
    private var isTrackingFinalized = false
    private var startTimestamp: String? = null
    private var showExitWarning by mutableStateOf(false)

    /**
     * Player
     */
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private lateinit var playerApi: PlayerApi

    /**
     * Listeners
     */
    private val playbackStateListener: Player.Listener = playbackStateListener()

    private val subtitleFileSuspendLauncher = registerForSuspendActivityResult(OpenDocument())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply initial orientation before content is set to avoid visual glitch
        val screenOrientation = viewModel.uiState.value.playerPreferences?.playerScreenOrientation
        if (screenOrientation != null && requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            requestedOrientation = when (screenOrientation) {
                ScreenOrientation.AUTOMATIC -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                ScreenOrientation.LANDSCAPE_REVERSE -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                ScreenOrientation.LANDSCAPE_AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                ScreenOrientation.VIDEO_ORIENTATION -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED // Will be set by RotationState after metadata
            }
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var player by remember { mutableStateOf<MediaController?>(null) }

            LifecycleStartEffect(Unit) {
                maybeInitControllerFuture()
                lifecycleScope.launch {
                    player = controllerFuture?.await()
                }

                onStopOrDispose {
                    player = null
                }
            }

            CompositionLocalProvider(LocalUseMaterialYouControls provides (uiState.playerPreferences?.useMaterialYouControls == true)) {
                NextPlayerTheme(darkTheme = true) {
                    MediaPlayerScreen(
                        player = player,
                        viewModel = viewModel,
                        playerPreferences = uiState.playerPreferences ?: return@NextPlayerTheme,
                        onSelectSubtitleClick = {
                            lifecycleScope.launch {
                                val uri = subtitleFileSuspendLauncher.launch(
                                    arrayOf(
                                        MimeTypes.APPLICATION_SUBRIP,
                                        MimeTypes.APPLICATION_TTML,
                                        MimeTypes.TEXT_VTT,
                                        MimeTypes.TEXT_SSA,
                                        MimeTypes.BASE_TYPE_APPLICATION + "/octet-stream",
                                        MimeTypes.BASE_TYPE_TEXT + "/*",
                                    ),
                                ) ?: return@launch
                                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                maybeInitControllerFuture()
                                controllerFuture?.await()?.addSubtitleTrack(uri)
                            }
                        },
                        onBackClick = {
                            if (startTimestamp != null) {
                                showExitWarning = true
                            } else {
                                finishAndStopPlayerSession()
                            }
                        },
                        onPlayInBackgroundClick = {
                            playInBackground = true
                            finish()
                        },
                    )

                    if (showExitWarning) {
                        NextDialog(
                            onDismissRequest = { showExitWarning = false },
                            title = { Text(stringResource(dev.anilbeesetti.nextplayer.core.ui.R.string.exit_warning_title)) },
                            content = { Text(stringResource(dev.anilbeesetti.nextplayer.core.ui.R.string.exit_warning_message)) },
                            confirmButton = {
                                TextButton(onClick = {
                                    showExitWarning = false
                                    if (journalId != null && materialIndex != -1 && startTimestamp != null && !isTrackingFinalized) {
                                        val endTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                                        val capturedJournalId = journalId!!
                                        val capturedIndex = materialIndex
                                        isTrackingFinalized = true
                                        lifecycleScope.launch {
                                            viewModel.finalizeMaterialTracking(capturedJournalId, capturedIndex, endTimestamp)
                                            finishAndStopPlayerSession()
                                        }
                                    } else {
                                        finishAndStopPlayerSession()
                                    }
                                }) {
                                    Text(stringResource(dev.anilbeesetti.nextplayer.core.ui.R.string.exit))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showExitWarning = false }) {
                                    Text(stringResource(dev.anilbeesetti.nextplayer.core.ui.R.string.continue_button))
                                }
                            }
                        )
                    }
                }
            }
        }

        playerApi = PlayerApi(this)
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            maybeInitControllerFuture()
            mediaController = controllerFuture?.await()

            mediaController?.run {
                updateKeepScreenOnFlag()
                addListener(playbackStateListener)
                startPlayback()
            }
        }
    }

    override fun onStop() {
        mediaController?.run {
            viewModel.playWhenReady = playWhenReady
            removeListener(playbackStateListener)
        }
        val shouldPlayInBackground = playInBackground || playerPreferences?.autoBackgroundPlay == true
        if (subtitleFileSuspendLauncher.isAwaitingResult || !shouldPlayInBackground) {
            mediaController?.pause()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) {
            finish()
            if (!shouldPlayInBackground) {
                mediaController?.stopPlayerSession()
            }
        }

        controllerFuture?.run {
            MediaController.releaseFuture(this)
            controllerFuture = null
        }
        super.onStop()
    }

    private fun maybeInitControllerFuture() {
        if (controllerFuture == null) {
            val sessionToken = SessionToken(applicationContext, ComponentName(applicationContext, PlayerService::class.java))
            controllerFuture = MediaController.Builder(applicationContext, sessionToken).buildAsync()
        }
    }

    private fun startPlayback() {
        val uri = intent.data ?: return
        journalId = intent.getStringExtra(PlayerApi.API_JOURNAL_ID)
        val initialIndex = intent.getIntExtra(PlayerApi.API_JOURNAL_MATERIAL_INDEX, -1)

        if (journalId != null && initialIndex != -1) {
            pendingMaterialIndex = initialIndex
            // materialIndex remains -1 until confirmed READY
        }

        val returningFromBackground = !isIntentNew && mediaController?.currentMediaItem != null
        val isNewUriTheCurrentMediaItem = mediaController?.currentMediaItem?.localConfiguration?.uri.toString() == uri.toString()

        if (returningFromBackground || isNewUriTheCurrentMediaItem) {
            mediaController?.prepare()
            mediaController?.playWhenReady = viewModel.playWhenReady
            mediaController?.currentMediaItem?.localConfiguration?.uri?.let {
                viewModel.loadVideoNotes(it, this@PlayerActivity, journalId, materialIndex)
            }
            return
        }

        isIntentNew = false

        lifecycleScope.launch {
            playVideo(uri)
        }
    }

    private suspend fun playVideo(uri: Uri) = withContext(Dispatchers.Default) {
        val mediaContentUri = getMediaContentUri(uri)
        val playlist = playerApi.getPlaylist().takeIf { it.isNotEmpty() }
            ?: mediaContentUri?.let { mediaUri ->
                viewModel.getPlaylistFromUri(mediaUri)
                    .map { it.uriString }
                    .toMutableList()
                    .apply {
                        if (!contains(mediaUri.toString())) {
                            add(index = 0, element = mediaUri.toString())
                        }
                    }
            } ?: listOf(uri.toString())

        val mediaItemIndexToPlay = playlist.indexOfFirst {
            it == (mediaContentUri ?: uri).toString()
        }.takeIf { it >= 0 } ?: 0

        val mediaItems = playlist.mapIndexed { index, uri ->
            MediaItem.Builder().apply {
                setUri(uri)
                setMediaId(uri)
                if (index == mediaItemIndexToPlay) {
                    setMediaMetadata(
                        MediaMetadata.Builder().apply {
                            setTitle(playerApi.title)
                            setExtras(positionMs = playerApi.position?.toLong())
                        }.build(),
                    )
                    val apiSubs = playerApi.getSubs().map { subtitle ->
                        uriToSubtitleConfiguration(
                            uri = subtitle.uri,
                            subtitleEncoding = playerPreferences?.subtitleTextEncoding ?: "",
                            isSelected = subtitle.isSelected,
                        )
                    }
                    setSubtitleConfigurations(apiSubs)
                }
            }.build()
        }

        withContext(Dispatchers.Main) {
            mediaController?.run {
                stop()
                clearMediaItems()
                setMediaItems(mediaItems, mediaItemIndexToPlay, playerApi.position?.toLong() ?: C.TIME_UNSET)
                playWhenReady = viewModel.playWhenReady
                prepare()

                currentMediaItem?.localConfiguration?.uri?.let {
                    val indexToUse = if (pendingMaterialIndex != -1) pendingMaterialIndex else materialIndex
                    viewModel.loadVideoNotes(it, this@PlayerActivity, journalId, indexToUse)
                }
            }
        }
    }

    private fun playbackStateListener() = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            val uri = mediaItem?.localConfiguration?.uri
            intent.data = uri
            if (uri != null) {
                val indexToUse = if (pendingMaterialIndex != -1) pendingMaterialIndex else materialIndex
                viewModel.loadVideoNotes(uri, this@PlayerActivity, journalId, indexToUse)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            updateKeepScreenOnFlag()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            when (playbackState) {
                Player.STATE_READY -> {
                    if (pendingMaterialIndex != -1 && journalId != null) {
                        materialIndex = pendingMaterialIndex
                        pendingMaterialIndex = -1

                        val current = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        startTimestamp = current
                        isTrackingFinalized = false

                        val capturedJournalId = journalId!!
                        val capturedIndex = materialIndex
                        lifecycleScope.launch {
                            viewModel.updateMaterialTracking(capturedJournalId, capturedIndex, "$current-")
                        }

                        // Reload notes with confirmed index
                        mediaController?.currentMediaItem?.localConfiguration?.uri?.let {
                            viewModel.loadVideoNotes(it, this@PlayerActivity, journalId, materialIndex)
                        }
                    }
                }
                Player.STATE_ENDED -> {
                    isPlaybackFinished = mediaController?.playbackState == Player.STATE_ENDED
                    onMaterialEnded()
                }

                else -> {}
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            if (journalId != null && (materialIndex != -1 || pendingMaterialIndex != -1)) {
                onMaterialEnded()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)

            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                if (mediaController?.repeatMode != Player.REPEAT_MODE_OFF) return
                isPlaybackFinished = true
                onMaterialEnded()
            }
        }
    }

    private var isProcessingEnd = false

    private fun onMaterialEnded() {
        if (isProcessingEnd) return
        isProcessingEnd = true

        val capturedJournalId = journalId
        val capturedMaterialIndex = materialIndex
        val capturedStartTimestamp = startTimestamp

        if (capturedJournalId != null && capturedMaterialIndex != -1 && capturedStartTimestamp != null && !isTrackingFinalized) {
            isTrackingFinalized = true
            val endTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            Logger.logDebug("PlayerActivity", "Journal material ended: $capturedJournalId, material $capturedMaterialIndex. Tracking: $capturedStartTimestamp$endTimestamp")
            lifecycleScope.launch {
                viewModel.finalizeMaterialTracking(capturedJournalId, capturedMaterialIndex, endTimestamp)
                startTimestamp = null
                processNextOrFinish(capturedJournalId, capturedMaterialIndex)
            }
        } else {
            lifecycleScope.launch {
                processNextOrFinish(capturedJournalId ?: "", capturedMaterialIndex)
            }
        }
    }

    private suspend fun processNextOrFinish(capturedJournalId: String, capturedMaterialIndex: Int) {
        val shouldAutoPlay = viewModel.uiState.value.applicationPreferences?.autoPlayNextMaterial == true
        val isError = mediaController?.playerError != null || !isPlaybackFinished

        if (shouldAutoPlay && !isError && capturedJournalId.isNotEmpty() && capturedMaterialIndex != -1) {
            val syncData = viewModel.getSyncData()
            val journal = syncData?.journals?.find { it.id == capturedJournalId }
            val nextMaterialIndex = capturedMaterialIndex + 1

            if (journal != null && nextMaterialIndex < journal.materiales.size) {
                val nextMaterial = journal.materiales[nextMaterialIndex]
                val nextPath = nextMaterial["path"]?.jsonPrimitive?.contentOrNull
                val nextTitle = nextMaterial["title_material"]?.jsonPrimitive?.contentOrNull
                val nextUri = if (!nextPath.isNullOrEmpty()) {
                    val recursosUri = viewModel.uiState.value.applicationPreferences?.recursosUri
                    if (recursosUri != null) {
                        val treeUri = Uri.parse(recursosUri)
                        treeUri.findFileByPath(this@PlayerActivity, nextPath)?.uri
                    } else null
                } else null

                if (nextUri != null) {
                    pendingMaterialIndex = nextMaterialIndex
                    isProcessingEnd = false
                    isTrackingFinalized = false
                    withContext(Dispatchers.Main) {
                        playVideo(nextUri)
                    }
                    return
                } else if (nextTitle?.equals("[User selection]", ignoreCase = true) == true) {
                    withContext(Dispatchers.Main) {
                        finishAndStopPlayerSession()
                    }
                    return
                }
            }
        }
        withContext(Dispatchers.Main) {
            finishAndStopPlayerSession()
        }
    }

    override fun finish() {
        if (playerApi.shouldReturnResult) {
            val result = playerApi.getResult(
                isPlaybackFinished = isPlaybackFinished,
                duration = mediaController?.duration ?: C.TIME_UNSET,
                position = mediaController?.currentPosition ?: C.TIME_UNSET,
            )
            setResult(RESULT_OK, result)
        }
        super.finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.data != null) {
            setIntent(intent)
            isIntentNew = true
            if (mediaController != null) {
                startPlayback()
            }
        }
    }

    private fun updateKeepScreenOnFlag() {
        if (mediaController?.isPlaying == true) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun finishAndStopPlayerSession() {
        finish()
        mediaController?.stopPlayerSession()
    }

    override fun onWindowAttributesChanged(params: WindowManager.LayoutParams?) {
        super.onWindowAttributesChanged(params)
        for (listener in onWindowAttributesChangedListener) {
            listener.accept(params)
        }
    }

    fun addOnWindowAttributesChangedListener(listener: Consumer<WindowManager.LayoutParams?>) {
        onWindowAttributesChangedListener.add(listener)
    }

    fun removeOnWindowAttributesChangedListener(listener: Consumer<WindowManager.LayoutParams?>) {
        onWindowAttributesChangedListener.remove(listener)
    }
}
