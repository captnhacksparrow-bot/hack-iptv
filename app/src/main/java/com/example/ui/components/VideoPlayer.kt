package com.example.ui.components

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay

import kotlinx.coroutines.flow.SharedFlow

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    isInPipMode: Boolean = false,
    videoUrl: String,
    title: String,
    subtitle: String?,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLiveStream: Boolean = false,
    remoteCommands: SharedFlow<String>? = null,
    onPlaybackStarted: () -> Unit = {}
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    val currentOnPlaybackStarted by rememberUpdatedState(onPlaybackStarted)

    LaunchedEffect(exoPlayer, remoteCommands) {
        if (exoPlayer != null && remoteCommands != null) {
            remoteCommands.collect { cmd ->
                when (cmd) {
                    "PLAY_PAUSE" -> {
                        exoPlayer?.let { player ->
                            player.playWhenReady = !player.playWhenReady
                        }
                    }
                    "VOLUME_UP" -> {
                        exoPlayer?.let { player ->
                            val currentVol = player.volume
                            player.volume = (currentVol + 0.1f).coerceAtMost(1f)
                        }
                    }
                    "VOLUME_DOWN" -> {
                        exoPlayer?.let { player ->
                            val currentVol = player.volume
                            player.volume = (currentVol - 0.1f).coerceAtLeast(0f)
                        }
                    }
                    "MUTE" -> {
                        exoPlayer?.let { player ->
                            player.volume = if (player.volume > 0f) 0f else 1f
                        }
                    }
                    "FORWARD" -> {
                        exoPlayer?.let { player ->
                            val currentPosition = player.currentPosition
                            player.seekTo((currentPosition + 10000).coerceAtMost(player.duration))
                        }
                    }
                    "REWIND" -> {
                        exoPlayer?.let { player ->
                            val currentPosition = player.currentPosition
                            player.seekTo((currentPosition - 10000).coerceAtLeast(0))
                        }
                    }
                    "FULLSCREEN" -> {
                        resizeMode = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    }
                }
            }
        }
    }

    // Re-initialize player or change source when videoUrl changes
    LaunchedEffect(videoUrl) {
        if (exoPlayer == null) {
            val player = ExoPlayer.Builder(context).build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            currentOnPlaybackStarted()
                        }
                    }
                })
            }
            exoPlayer = player
        }

        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            val mediaItem = MediaItem.fromUri(videoUrl)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    // Clean up on dispose
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
    ) {
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        this.resizeMode = resizeMode
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.resizeMode = resizeMode
                    view.useController = !isInPipMode
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Normal details overlay
        if (!isInPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
            // Text details on top-left
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.6f), shape = MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }

            // Quick actions on top-right
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color.Black.copy(alpha = 0.6f), shape = MaterialTheme.shapes.small)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Resize mode button
                IconButton(
                    onClick = {
                        resizeMode = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Aspect Ratio Mode",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Download/Record button
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isLiveStream) Icons.Default.RadioButtonChecked else Icons.Default.Download,
                        contentDescription = if (isLiveStream) "Record Stream" else "Download Stream",
                        tint = if (isLiveStream) Color.Red else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        } // Close if (!isInPipMode)
    }
}
