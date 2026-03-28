package dev.anilbeesetti.nextplayer.settings.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.anilbeesetti.nextplayer.core.model.VideoNotesPosition
import dev.anilbeesetti.nextplayer.core.ui.R

@Composable
fun VideoNotesPosition.name() = when (this) {
    VideoNotesPosition.LEFT -> stringResource(id = R.string.control_buttons_alignment_left)
    VideoNotesPosition.RIGHT -> stringResource(id = R.string.control_buttons_alignment_right)
    VideoNotesPosition.BOTTOM -> stringResource(id = R.string.bottom)
}
