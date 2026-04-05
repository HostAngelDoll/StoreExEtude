package dev.anilbeesetti.nextplayer.settings.screens.journals

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.*
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalPreferencesRoute(
    viewModel: JournalPreferencesViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    JournalPreferencesScreen(
        uiState = uiState,
        onSyncOnAppStartChange = viewModel::updateSyncOnAppStart,
        onRecursosUriChange = viewModel::updateRecursosUri,
        onJornadasUriChange = viewModel::updateJornadasUri,
        onManualIpChange = viewModel::updateManualServerIp,
        onPortChange = viewModel::updateServerPort,
        onSyncClick = viewModel::sync,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JournalPreferencesScreen(
    uiState: dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences,
    onSyncOnAppStartChange: (Boolean) -> Unit,
    onRecursosUriChange: (String?) -> Unit,
    onJornadasUriChange: (String?) -> Unit,
    onManualIpChange: (String?) -> Unit,
    onPortChange: (Int) -> Unit,
    onSyncClick: () -> Unit,
    onNavigateUp: () -> Unit,
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    val resourcesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            onRecursosUriChange(it.toString())
        }
    }

    val journalsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            onJornadasUriChange(it.toString())
        }
    }

    var showIpDialog by remember { mutableStateOf(false) }
    var showPortDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.manage_journals),
                navigationIcon = {
                    FilledTonalIconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSyncClick) {
                        Icon(
                            imageVector = NextIcons.Sync,
                            contentDescription = stringResource(R.string.sync),
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
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            PreferenceSwitch(
                title = stringResource(id = R.string.sync_on_app_start),
                isChecked = uiState.syncOnAppStart,
                onClick = { onSyncOnAppStartChange(!uiState.syncOnAppStart) },
                isFirstItem = true,
            )

            ClickablePreferenceItem(
                title = stringResource(id = R.string.resources_folder),
                description = uiState.recursosUri ?: stringResource(id = R.string.select_folder),
                onClick = { resourcesLauncher.launch(null) },
            )

            ClickablePreferenceItem(
                title = stringResource(id = R.string.journals_folder),
                description = uiState.jornadasUri ?: stringResource(id = R.string.select_folder),
                onClick = { journalsLauncher.launch(null) },
            )

            ClickablePreferenceItem(
                title = stringResource(id = R.string.manual_server_ip),
                description = uiState.manualServerIp ?: stringResource(id = R.string.enter_ip),
                onClick = { showIpDialog = true },
            )

            ClickablePreferenceItem(
                title = stringResource(id = R.string.server_port),
                description = uiState.serverPort.toString(),
                onClick = { showPortDialog = true },
            )

            PreferenceItem(
                title = stringResource(id = R.string.last_server_ip),
                description = uiState.lastServerIp ?: stringResource(id = R.string.none),
                isLastItem = true,
                enabled = true,
            )
        }
    }

    if (showIpDialog) {
        var ipText by remember { mutableStateOf(uiState.manualServerIp ?: "") }
        NextDialog(
            onDismissRequest = { showIpDialog = false },
            title = { Text(stringResource(id = R.string.manual_server_ip)) },
            content = {
                TextField(
                    value = ipText,
                    onValueChange = { ipText = it },
                    placeholder = { Text("192.168.1.100") },
                    modifier = Modifier.padding(top = 16.dp),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onManualIpChange(ipText.ifBlank { null })
                    showIpDialog = false
                }) {
                    Text(stringResource(id = R.string.okay))
                }
            },
            dismissButton = {
                TextButton(onClick = { showIpDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            },
        )
    }

    if (showPortDialog) {
        var portText by remember { mutableStateOf(uiState.serverPort.toString()) }
        NextDialog(
            onDismissRequest = { showPortDialog = false },
            title = { Text(stringResource(id = R.string.server_port)) },
            content = {
                TextField(
                    value = portText,
                    onValueChange = { portText = it },
                    modifier = Modifier.padding(top = 16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    portText.toIntOrNull()?.let { onPortChange(it) }
                    showPortDialog = false
                }) {
                    Text(stringResource(id = R.string.okay))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPortDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            },
        )
    }
}
