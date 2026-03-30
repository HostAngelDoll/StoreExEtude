package dev.anilbeesetti.nextplayer.settings.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.anilbeesetti.nextplayer.core.model.VideoTextPanelPosition
import dev.anilbeesetti.nextplayer.core.ui.R

@Composable
fun VideoTextPanelPosition.name() = when (this) {
    VideoTextPanelPosition.VIDEO_ABOVE_TEXT_BELOW -> stringResource(id = R.string.video_text_panel_position_vertical)
    VideoTextPanelPosition.VIDEO_LEFT_TEXT_RIGHT -> stringResource(id = R.string.video_text_panel_position_horizontal)
}
