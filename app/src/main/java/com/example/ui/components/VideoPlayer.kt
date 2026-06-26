package com.example.ui.components

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Download
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

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    title: String,
    subtitle: String?,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLiveStream: Boolean = false
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // Ad State
    var isAdPlaying by remember { mutableStateOf(false) }
    var adCountdown by remember { mutableStateOf(5) }

    val adTitle = "The Caduceus Method (Cut the OM Nonsense)"
    val adSubtitle = "Stray Divinity - Premium Wellness Activation"

    // When videoUrl changes, reset ad playing state if it is a live stream
    LaunchedEffect(videoUrl) {
        if (isLiveStream) {
            isAdPlaying = true
            adCountdown = 5
        } else {
            isAdPlaying = false
        }
    }

    // Countdown Timer for Ad skip button
    LaunchedEffect(isAdPlaying, videoUrl) {
        if (isAdPlaying) {
            adCountdown = 5
            while (adCountdown > 0) {
                delay(1000)
                adCountdown--
            }
        }
    }

    // Re-initialize player or change source when videoUrl or isAdPlaying changes
    LaunchedEffect(videoUrl, isAdPlaying) {
        if (exoPlayer == null) {
            val player = ExoPlayer.Builder(context).build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
            exoPlayer = player
        }

        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            
            val currentUri = if (isAdPlaying) {
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
            } else {
                videoUrl
            }
            
            val mediaItem = MediaItem.fromUri(currentUri)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    // Listener to automatically end the ad when playback finishes
    val currentIsAdPlaying by rememberUpdatedState(isAdPlaying)
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (currentIsAdPlaying) {
                        isAdPlaying = false
                    }
                }
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
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
            .fillMaxWidth()
            .height(240.dp)
            .background(Color.Black)
    ) {
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = !isAdPlaying
                        this.resizeMode = resizeMode
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.resizeMode = resizeMode
                    view.useController = !isAdPlaying
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Custom Overlay for styling/actions
        if (isAdPlaying) {
            // High-end Ad overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(12.dp)
            ) {
                // Ad Badge & Title top-left
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.75f), shape = MaterialTheme.shapes.small)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color(0xFFFFB03A), // Gold brand color
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "AD",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF07090C),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = adTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = adSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }

                // Skip Ad Button & Countdown bottom-right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                ) {
                    Button(
                        onClick = { isAdPlaying = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (adCountdown <= 0) Color(0xFFFFB03A) else Color.DarkGray,
                            contentColor = if (adCountdown <= 0) Color(0xFF07090C) else Color.LightGray
                        ),
                        shape = MaterialTheme.shapes.small,
                        enabled = adCountdown <= 0,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("skip_ad_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (adCountdown > 0) "Skip Ad in ${adCountdown}s" else "Skip Ad",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            if (adCountdown <= 0) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Skip",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Normal details overlay
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
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Stream",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
