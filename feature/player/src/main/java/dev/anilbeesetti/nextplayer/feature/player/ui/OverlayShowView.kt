package dev.anilbeesetti.nextplayer.feature.player.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Player
import dev.anilbeesetti.nextplayer.core.model.LoopMode
import dev.anilbeesetti.nextplayer.core.model.VideoContentScale
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.feature.player.extensions.noRippleClickable
import dev.anilbeesetti.nextplayer.feature.player.state.SubtitleOptionsEvent

@Composable
fun BoxScope.OverlayShowView(
    player: Player,
    overlayView: PlayerOverlay?,
    videoContentScale: VideoContentScale,
    onDismiss: () -> Unit = {},
    viewModel: dev.anilbeesetti.nextplayer.feature.player.PlayerViewModel,
    onSelectSubtitleClick: () -> Unit = {},
    onSubtitleOptionEvent: (SubtitleOptionsEvent) -> Unit = {},
    onVideoContentScaleChanged: (VideoContentScale) -> Unit = {},
    onAudioClick: () -> Unit = {},
    onSubtitleClick: () -> Unit = {},
    onPlaybackSpeedClick: () -> Unit = {},
    onPlaylistClick: () -> Unit = {},
    onOsdSettingsClick: () -> Unit = {},
    onPictureInPictureClick: () -> Unit = {},
    onPlayInBackgroundClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .then(
                if (overlayView != null) {
                    Modifier.noRippleClickable(onClick = onDismiss)
                } else {
                    Modifier
                },
            ),
    )

    AudioTrackSelectorView(
        show = overlayView == PlayerOverlay.AUDIO_SELECTOR,
        player = player,
        onDismiss = onDismiss,
    )

    SubtitleSelectorView(
        show = overlayView == PlayerOverlay.SUBTITLE_SELECTOR,
        player = player,
        onSelectSubtitleClick = onSelectSubtitleClick,
        onEvent = onSubtitleOptionEvent,
        onDismiss = onDismiss,
    )

    PlaybackSpeedSelectorView(
        show = overlayView == PlayerOverlay.PLAYBACK_SPEED,
        player = player,
    )

    VideoContentScaleSelectorView(
        show = overlayView == PlayerOverlay.VIDEO_CONTENT_SCALE,
        videoContentScale = videoContentScale,
        onVideoContentScaleChanged = onVideoContentScaleChanged,
        onDismiss = onDismiss,
    )

    PlaylistView(
        show = overlayView == PlayerOverlay.PLAYLIST,
        player = player,
    )

    OSDSettingsView(
        show = overlayView == PlayerOverlay.OSD_SETTINGS,
        viewModel = viewModel,
    )

    val subtitleIcon = painterResource(R.drawable.ic_subtitle_track)
    val subtitleLabel = stringResource(R.string.subtitle)
    val audioIcon = painterResource(R.drawable.ic_audio_track)
    val audioLabel = stringResource(R.string.audio)
    val speedIcon = painterResource(R.drawable.ic_speed)
    val speedLabel = stringResource(R.string.speed)
    val playlistIcon = painterResource(R.drawable.ic_playlist)
    val playlistLabel = stringResource(R.string.now_playing)
    val osdIcon = painterResource(R.drawable.ic_tune)
    val osdLabel = stringResource(R.string.player_osd)
    val pipIcon = painterResource(R.drawable.ic_pip)
    val pipLabel = stringResource(R.string.picture_in_picture)
    val backgroundIcon = painterResource(R.drawable.ic_headset)
    val backgroundLabel = stringResource(R.string.background_play)
    val repeatIcon = painterResource(
        when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> R.drawable.ic_loop_one
            Player.REPEAT_MODE_ALL -> R.drawable.ic_loop_all
            else -> R.drawable.ic_loop_off
        }
    )
    val repeatLabel = stringResource(R.string.loop_mode)
    val shuffleIcon = painterResource(if (player.shuffleModeEnabled) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle)
    val shuffleLabel = stringResource(R.string.shuffle)

    val overflowMenuItems = remember(
        player.repeatMode,
        player.shuffleModeEnabled,
        subtitleIcon, subtitleLabel,
        audioIcon, audioLabel,
        speedIcon, speedLabel,
        playlistIcon, playlistLabel,
        osdIcon, osdLabel,
        pipIcon, pipLabel,
        backgroundIcon, backgroundLabel,
        repeatIcon, repeatLabel,
        shuffleIcon, shuffleLabel
    ) {
        listOf(
            OverflowMenuItem(
                icon = subtitleIcon,
                label = subtitleLabel,
                onClick = onSubtitleClick,
            ),
            OverflowMenuItem(
                icon = audioIcon,
                label = audioLabel,
                onClick = onAudioClick,
            ),
            OverflowMenuItem(
                icon = speedIcon,
                label = speedLabel,
                onClick = onPlaybackSpeedClick,
            ),
            OverflowMenuItem(
                icon = playlistIcon,
                label = playlistLabel,
                onClick = onPlaylistClick,
            ),
            OverflowMenuItem(
                icon = osdIcon,
                label = osdLabel,
                onClick = onOsdSettingsClick,
            ),
            OverflowMenuItem(
                icon = pipIcon,
                label = pipLabel,
                onClick = onPictureInPictureClick,
            ),
            OverflowMenuItem(
                icon = backgroundIcon,
                label = backgroundLabel,
                onClick = onPlayInBackgroundClick,
            ),
            OverflowMenuItem(
                icon = repeatIcon,
                label = repeatLabel,
                onClick = {
                    val nextMode = when (player.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                        Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                        else -> Player.REPEAT_MODE_OFF
                    }
                    player.repeatMode = nextMode
                    viewModel.setLoopMode(
                        when (nextMode) {
                            Player.REPEAT_MODE_ONE -> LoopMode.ONE
                            Player.REPEAT_MODE_ALL -> LoopMode.ALL
                            else -> LoopMode.OFF
                        }
                    )
                },
            ),
            OverflowMenuItem(
                icon = shuffleIcon,
                label = shuffleLabel,
                onClick = {
                    player.shuffleModeEnabled = !player.shuffleModeEnabled
                },
            ),
        )
    }

    PlaybackOverflowMenu(
        show = overlayView == PlayerOverlay.OVERFLOW_MENU,
        items = overflowMenuItems,
        onDismiss = onDismiss,
    )
}

val Configuration.isPortrait: Boolean
    get() = orientation == Configuration.ORIENTATION_PORTRAIT

enum class PlayerOverlay {
    AUDIO_SELECTOR,
    SUBTITLE_SELECTOR,
    PLAYBACK_SPEED,
    VIDEO_CONTENT_SCALE,
    PLAYLIST,
    OSD_SETTINGS,
    OVERFLOW_MENU,
}
