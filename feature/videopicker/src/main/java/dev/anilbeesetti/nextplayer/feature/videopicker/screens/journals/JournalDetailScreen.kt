package dev.anilbeesetti.nextplayer.feature.videopicker.screens.journals

import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import coil3.compose.AsyncImage
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.CenterCircularProgressBar
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.InfoChip

@Composable
fun JournalDetailRoute(
    viewModel: JournalDetailViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
    onPlayVideo: (Uri) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    JournalDetailScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onExecuteClick = { viewModel.executeJournal(onPlayVideo) },
        onResetClick = viewModel::resetJournal,
        onDownloadClick = viewModel::downloadMaterials,
        onStopDownloadClick = viewModel::stopDownloads,
        onUploadClick = { /* Future implementation */ }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalDetailScreen(
    uiState: JournalDetailUiState,
    onNavigateUp: () -> Unit,
    onExecuteClick: () -> Unit,
    onResetClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onStopDownloadClick: () -> Unit,
    onUploadClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            NextTopAppBar(
                title = uiState.name,
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_up),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            CenterCircularProgressBar()
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.error, color = MaterialTheme.colorScheme.error)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                JournalHeaderInfo(uiState)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.materials, key = { it.index }) { material ->
                        MaterialItem(material)
                    }
                }

                if (uiState.isDownloading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Progreso jornada",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(
                            progress = { uiState.overallProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Descargando: ${uiState.currentFileName ?: "..."}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LinearProgressIndicator(
                            progress = { uiState.fileProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                ActionButtons(
                    isDownloading = uiState.isDownloading,
                    canDownload = uiState.canDownload,
                    canExecute = uiState.canExecute && !uiState.isDownloading,
                    canReset = uiState.canReset && !uiState.isDownloading,
                    canUpload = uiState.canUpload && !uiState.isDownloading,
                    onDownloadClick = onDownloadClick,
                    onStopDownloadClick = onStopDownloadClick,
                    onExecuteClick = onExecuteClick,
                    onResetClick = onResetClick,
                    onUploadClick = onUploadClick
                )
            }
        }
    }
}

@Composable
fun JournalHeaderInfo(uiState: JournalDetailUiState) {
    val context = LocalContext.current
    val formattedExpectedDate = DateUtils.formatDateTime(
        context,
        uiState.expectedDate,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_WEEKDAY,
    )
    val formattedUpdatedAt = if (uiState.updatedAt > 0) {
        DateUtils.getRelativeTimeSpanString(
            uiState.updatedAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    } else {
        "Nunca"
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formattedExpectedDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = uiState.status,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Actualizado: $formattedUpdatedAt",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun MaterialItem(material: MaterialUiModel) {
    ListItem(
        headlineContent = {
            Text(
                text = material.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (material.hasUserSelection) FontWeight.SemiBold else FontWeight.Normal,
                color = if (material.hasUserSelection) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
            )
        },
        supportingContent = {
            Column {
                if (!material.summonPath.isNullOrEmpty()) {
                    Text(
                        text = material.summonPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (material.isDownloaded && material.originalFileName != null) {
                    Text(
                        text = material.originalFileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!material.isDownloaded && material.hasUserSelection) {
                    Text(
                        text = "Requiere descargar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (material.summonPath != null && material.missingFilesCount > 0) {
                    Text(
                        text = "Faltan archivos por descargar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (material.isDownloaded && material.duration != null) {
                        InfoChip(text = material.duration)
                    }
                    if (material.hasSidecar) {
                        Icon(
                            imageVector = NextIcons.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (material.isPlayed) {
                        Icon(
                            imageVector = NextIcons.Done,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 60.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (material.thumbnailUri != null) {
                    AsyncImage(
                        model = material.thumbnailUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = NextIcons.Video,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    )
}

@Composable
fun ActionButtons(
    isDownloading: Boolean,
    canDownload: Boolean,
    canExecute: Boolean,
    canReset: Boolean,
    canUpload: Boolean,
    onDownloadClick: () -> Unit,
    onStopDownloadClick: () -> Unit,
    onExecuteClick: () -> Unit,
    onResetClick: () -> Unit,
    onUploadClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isDownloading) {
            Button(
                onClick = onStopDownloadClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Detener descargas")
            }
        } else {
            Button(
                onClick = onDownloadClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = true
            ) {
                Text(if (canDownload) "Descargar materiales" else "Verificar existencias")
            }
        }
        Button(
            onClick = onExecuteClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = canExecute,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Text("Ejecutar jornada")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onResetClick,
                modifier = Modifier.weight(1f),
                enabled = canReset,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("Reiniciar")
            }
            Button(
                onClick = onUploadClick,
                modifier = Modifier.weight(1f),
                enabled = canUpload
            ) {
                Text("Subir terminada")
            }
        }
    }
}
