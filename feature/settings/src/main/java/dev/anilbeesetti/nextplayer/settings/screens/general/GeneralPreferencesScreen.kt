package dev.anilbeesetti.nextplayer.settings.screens.general

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.CancelButton
import dev.anilbeesetti.nextplayer.core.ui.components.ClickablePreferenceItem
import dev.anilbeesetti.nextplayer.core.ui.components.ListSectionTitle
import dev.anilbeesetti.nextplayer.core.ui.components.NextDialog
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons

@Composable
fun GeneralPreferencesScreen(
    onNavigateUp: () -> Unit,
    viewModel: GeneralPreferencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GeneralPreferencesContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GeneralPreferencesContent(
    uiState: GeneralPreferencesUiState,
    onEvent: (GeneralPreferencesUiEvent) -> Unit,
    onNavigateUp: () -> Unit,
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let { onEvent(GeneralPreferencesUiEvent.ExportSettings(contentResolver, it)) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { onEvent(GeneralPreferencesUiEvent.ShowDialog(GeneralPreferencesDialog.ImportSettingsConfirmationDialog(it))) }
    }

    val errorMessage = stringResource(R.string.import_settings_error)
    LaunchedEffect(uiState.showError) {
        if (uiState.showError) {
            snackbarHostState.showSnackbar(errorMessage)
            onEvent(GeneralPreferencesUiEvent.DismissError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.general_name),
                navigationIcon = {
                    FilledTonalIconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            ListSectionTitle(text = stringResource(id = R.string.user_data))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ClickablePreferenceItem(
                    title = stringResource(R.string.export_settings),
                    description = stringResource(R.string.export_settings_description),
                    icon = NextIcons.Share,
                    onClick = {
                        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                        exportLauncher.launch("nextplayer_settings_v1_$date.json")
                    },
                    isFirstItem = true,
                )
                ClickablePreferenceItem(
                    title = stringResource(R.string.import_settings),
                    description = stringResource(R.string.import_settings_description),
                    icon = NextIcons.FileOpen,
                    onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream")) },
                    isLastItem = true,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ClickablePreferenceItem(
                    title = stringResource(R.string.delete_thumbnail_cache),
                    description = stringResource(R.string.delete_thumbnail_cache_description),
                    icon = NextIcons.DeleteSweep,
                    onClick = { onEvent(GeneralPreferencesUiEvent.ShowDialog(GeneralPreferencesDialog.ClearThumbnailCacheDialog)) },
                    isFirstItem = true,
                )
                ClickablePreferenceItem(
                    title = stringResource(R.string.reset_settings),
                    description = stringResource(R.string.reset_settings_description),
                    icon = NextIcons.History,
                    onClick = { onEvent(GeneralPreferencesUiEvent.ShowDialog(GeneralPreferencesDialog.ResetSettingsDialog)) },
                    isLastItem = true,
                )
            }
        }

        uiState.showDialog?.let { dialog ->
            when (dialog) {
                GeneralPreferencesDialog.ClearThumbnailCacheDialog -> {
                    NextDialog(
                        onDismissRequest = { onEvent(GeneralPreferencesUiEvent.ShowDialog(null)) },
                        title = {
                            Text(
                                text = stringResource(R.string.delete_thumbnail_cache),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onEvent(GeneralPreferencesUiEvent.ClearThumbnailCache)
                                    onEvent(GeneralPreferencesUiEvent.ShowDialog(null))
                                },
                            ) {
                                Text(text = stringResource(R.string.delete))
                            }
                        },
                        dismissButton = { CancelButton(onClick = { onEvent(GeneralPreferencesUiEvent.ShowDialog(null)) }) },
                        content = {
                            Text(
                                text = stringResource(R.string.delete_thumbnail_cache_confirmation),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        },
                    )
                }
                GeneralPreferencesDialog.ResetSettingsDialog -> {
                    NextDialog(
                        onDismissRequest = { onEvent(GeneralPreferencesUiEvent.ShowDialog(null)) },
                        title = {
                            Text(
                                text = stringResource(R.string.reset_settings),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onEvent(GeneralPreferencesUiEvent.ResetSettings)
                                    onEvent(GeneralPreferencesUiEvent.ShowDialog(null))
                                },
                            ) {
                                Text(text = stringResource(R.string.reset))
                            }
                        },
                        dismissButton = { CancelButton(onClick = { onEvent(GeneralPreferencesUiEvent.ShowDialog(null)) }) },
                        content = {
                            Text(
                                text = stringResource(R.string.reset_settings_confirmation),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        },
                    )
                }
                is GeneralPreferencesDialog.ImportSettingsConfirmationDialog -> {
                    NextDialog(
                        onDismissRequest = { onEvent(GeneralPreferencesUiEvent.ShowDialog(null)) },
                        title = {
                            Text(
                                text = stringResource(R.string.import_settings),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onEvent(GeneralPreferencesUiEvent.ImportSettings(contentResolver, dialog.uri))
                                    onEvent(GeneralPreferencesUiEvent.ShowDialog(null))
                                },
                            ) {
                                Text(text = stringResource(R.string.import_action))
                            }
                        },
                        dismissButton = { CancelButton(onClick = { onEvent(GeneralPreferencesUiEvent.ShowDialog(null)) }) },
                        content = {
                            Text(
                                text = stringResource(R.string.import_settings_confirmation),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        },
                    )
                }
            }
        }
    }
}
