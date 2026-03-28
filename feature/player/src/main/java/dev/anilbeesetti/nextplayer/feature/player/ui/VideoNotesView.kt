package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.model.VideoNotesPosition
import dev.anilbeesetti.nextplayer.core.ui.R

enum class VideoNotesLayout {
    TOP, BOTTOM, LEFT, RIGHT
}

@Composable
fun VideoNotesView(
    notes: String,
    layout: VideoNotesLayout,
    sizeFraction: Float,
    onSizeChange: (Float) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isHorizontal = layout == VideoNotesLayout.TOP || layout == VideoNotesLayout.BOTTOM

    BoxWithConstraints(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f))
            .then(
                if (isHorizontal) {
                    Modifier.fillMaxWidth().fillMaxHeight(sizeFraction)
                } else {
                    Modifier.fillMaxHeight().fillMaxWidth(sizeFraction)
                }
            ),
    ) {
        val maxHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = null,
                    tint = Color.White,
                )
            }
            Text(
                text = notes,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp),
            )
            Spacer(modifier = Modifier.height(maxHeight))
        }

        // Draggable Handle
        Box(
            modifier = Modifier
                .then(
                    when (layout) {
                        VideoNotesLayout.TOP -> {
                            Modifier.fillMaxWidth().height(20.dp).align(Alignment.BottomCenter)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val delta = dragAmount.y / size.height.toFloat()
                                        onSizeChange((sizeFraction + delta).coerceIn(0.1f, 0.8f))
                                    }
                                }
                        }
                        VideoNotesLayout.BOTTOM -> {
                            Modifier.fillMaxWidth().height(20.dp).align(Alignment.TopCenter)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val delta = dragAmount.y / size.height.toFloat()
                                        onSizeChange((sizeFraction - delta).coerceIn(0.1f, 0.8f))
                                    }
                                }
                        }
                        VideoNotesLayout.LEFT -> {
                            Modifier.fillMaxHeight().width(20.dp).align(Alignment.CenterEnd)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val delta = dragAmount.x / size.width.toFloat()
                                        onSizeChange((sizeFraction + delta).coerceIn(0.1f, 0.8f))
                                    }
                                }
                        }
                        VideoNotesLayout.RIGHT -> {
                            Modifier.fillMaxHeight().width(20.dp).align(Alignment.CenterStart)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val delta = dragAmount.x / size.width.toFloat()
                                        onSizeChange((sizeFraction - delta).coerceIn(0.1f, 0.8f))
                                    }
                                }
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.5f), CircleShape)
                    .then(
                        if (isHorizontal) {
                            Modifier.width(40.dp).height(4.dp)
                        } else {
                            Modifier.width(4.dp).height(40.dp)
                        }
                    )
            )
        }
    }
}
