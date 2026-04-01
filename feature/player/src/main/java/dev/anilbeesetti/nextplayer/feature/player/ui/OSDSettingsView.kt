package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSlider
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSwitch
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.feature.player.PlayerViewModel

@Composable
fun BoxScope.OSDSettingsView(
    modifier: Modifier = Modifier,
    show: Boolean,
    viewModel: PlayerViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appPrefs = uiState.applicationPreferences ?: return

    OverlayView(
        modifier = modifier,
        show = show,
        title = stringResource(R.string.player_osd),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
                .padding(horizontal = 24.dp),
        ) {
            PreferenceSwitch(
                title = stringResource(id = R.string.show_player_osd),
                description = stringResource(id = R.string.show_player_osd_description),
                icon = NextIcons.Info,
                isChecked = appPrefs.showOSD,
                onClick = { viewModel.toggleShowOSD() },
                isFirstItem = true,
                isLastItem = !appPrefs.showOSD,
            )
            if (appPrefs.showOSD) {
                PreferenceSwitch(
                    title = stringResource(id = R.string.show_osd_duration),
                    icon = NextIcons.Timer,
                    isChecked = appPrefs.osdShowDuration,
                    onClick = { viewModel.toggleOsdShowDuration() },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.show_osd_remaining_time),
                    icon = NextIcons.Timer,
                    isChecked = appPrefs.osdShowRemainingTime,
                    onClick = { viewModel.toggleOsdShowRemainingTime() },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.show_osd_battery),
                    icon = NextIcons.Brightness,
                    isChecked = appPrefs.osdShowBattery,
                    onClick = { viewModel.toggleOsdShowBattery() },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.show_osd_clock),
                    icon = NextIcons.Timer,
                    isChecked = appPrefs.osdShowClock,
                    onClick = { viewModel.toggleOsdShowClock() },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.show_osd_background),
                    icon = NextIcons.Appearance,
                    isChecked = appPrefs.osdShowBackground,
                    onClick = { viewModel.toggleOsdShowBackground() },
                )
                PreferenceSlider(
                    title = stringResource(id = R.string.osd_margin),
                    description = stringResource(
                        id = R.string.osd_margin_description,
                        appPrefs.osdMarginPercent,
                    ),
                    icon = NextIcons.Size,
                    value = appPrefs.osdMarginPercent.toFloat(),
                    valueRange = 0f..20f,
                    onValueChange = { viewModel.updateOsdMarginPercent(it.toInt()) },
                    isLastItem = true,
                )
            }
        }
    }
}
