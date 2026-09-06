package com.example.ui.components
import androidx.compose.foundation.border
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun FloatingVideoPlayerOverlay(
    player: Player?,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onOpenMainPlayer: () -> Unit,
    onSwitchToMiniPlayer: () -> Unit,
    onAspectRatioChanged: (Float) -> Unit = {}
) {
    var title by remember { mutableStateOf(player?.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Unknown") }
    var isPlaying by remember { mutableStateOf(player?.isPlaying == true) }
    var repeatMode by remember { mutableIntStateOf(player?.repeatMode ?: Player.REPEAT_MODE_OFF) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsManager = remember { com.example.data.SettingsManager.getInstance(context) }
    val keepScreenAwake by settingsManager.keepScreenAwake.collectAsState()

    LaunchedEffect(player) {
        player?.videoSize?.let { vs ->
            if (vs.width > 0 && vs.height > 0) {
                onAspectRatioChanged(vs.width.toFloat() / vs.height.toFloat())
            }
        }
    }

    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                title = mediaItem?.mediaMetadata?.title?.toString() ?: "Unknown"
            }
            override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                isPlaying = isPlayingChange
            }
            override fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
            }
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    onAspectRatioChanged(videoSize.width.toFloat() / videoSize.height.toFloat())
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Topbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { onOpenMainPlayer() }
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.DragIndicator, contentDescription = "Drag", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onSwitchToMiniPlayer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = "Switch to Mini Player",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Video Player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                var showControls by remember { mutableStateOf(false) }
                if (player != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                useController = false
                                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            }
                        },
                        update = { view ->
                            if (view.player != player) {
                                view.player = player
                            }
                            view.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                            view.keepScreenOn = keepScreenAwake && isPlaying
                        },
                        modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { showControls = !showControls }
                            )
                        }
                    )
                    
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showControls,
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, end = 4.dp, bottom = 0.dp)
                            ) {
                                com.example.ui.screens.PlaybackProgressRow(
                                    mediaController = player,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 0.dp)
                                )
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 4.dp, end = 4.dp, top = 0.dp, bottom = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left/Center Playback controls
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        IconButton(
                                            onClick = { 
                                                player?.let { controller ->
                                                    if (controller.hasPreviousMediaItem()) {
                                                        controller.seekToPreviousMediaItem()
                                                    } else {
                                                        controller.seekTo(0)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.SkipPrevious,
                                                contentDescription = "Previous",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { 
                                                player?.let { controller ->
                                                    if (controller.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                                                        controller.seekTo(0)
                                                        controller.prepare()
                                                        controller.play()
                                                    } else if (controller.playbackState == androidx.media3.common.Player.STATE_IDLE) {
                                                        controller.prepare()
                                                        controller.play()
                                                    } else if (controller.isPlaying) {
                                                        controller.pause()
                                                    } else {
                                                        controller.play()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                contentDescription = "Play/Pause",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        
                                        IconButton(
                                            onClick = { 
                                                player?.let { controller ->
                                                    if (controller.hasNextMediaItem()) {
                                                        controller.seekToNextMediaItem()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.SkipNext,
                                                contentDescription = "Next",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                player?.let { controller ->
                                                    val nextMode = when (controller.repeatMode) {
                                                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                                        else -> Player.REPEAT_MODE_OFF
                                                    }
                                                    controller.repeatMode = nextMode
                                                    repeatMode = nextMode
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            val loopIcon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat
                                            val loopTint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                                            Icon(
                                                imageVector = loopIcon,
                                                contentDescription = "Loop Mode",
                                                tint = loopTint,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Right Action buttons (Close, Minimize, and placeholder space for corner Resize)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End)
                                    ) {
                                        IconButton(onClick = onMinimize, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Filled.Remove, "Minimize", tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Filled.Close, "Close completely", tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                        // Reserve 32dp spacing so buttons do not overlap sticky corner resize handle
                                        Spacer(modifier = Modifier.width(32.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sticky Corner Resize handle strictly positioned at BottomEnd
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onResize(dragAmount.x, dragAmount.y)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.ZoomOutMap,
                "Resize",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
