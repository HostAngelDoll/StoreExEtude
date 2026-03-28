package dev.anilbeesetti.nextplayer.settings.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.anilbeesetti.nextplayer.core.model.VideoNotesPosition
import dev.anilbeesetti.nextplayer.core.ui.R

@Composable
fun VideoNotesPosition.name() = when (this) {
    VideoNotesPosition.START -> stringResource(id = R.string.video_notes_position_start)
    VideoNotesPosition.END -> stringResource(id = R.string.video_notes_position_end)
}
