package com.example.ui

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ChannelEntity
import com.example.data.DownloadEntity
import com.example.data.EpgProgramEntity
import com.example.data.PlaylistEntity
import com.example.data.StreamType
import com.example.data.getStreamType
import com.example.ui.components.VideoPlayer
import com.example.ui.components.CaptnHackLogo
import com.example.ui.components.CategorySidebar
import com.example.viewmodel.IptvViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IptvDashboard(
    isInPipMode: Boolean = false,
    viewModel: IptvViewModel,
    modifier: Modifier = Modifier
) {
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val activePlayUrl by viewModel.activePlayUrl.collectAsState()
    val isPlayingCatchup by viewModel.isPlayingCatchup.collectAsState()
    val selectedProgram by viewModel.selectedProgram.collectAsState()
    val showPlaylistSelectorOnStart by viewModel.showPlaylistSelectorOnStart.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val deviceMode by viewModel.deviceMode.collectAsState()
    val autoFullscreen by viewModel.autoFullscreen.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) }
    var isUiVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    // Live TV Overlay state
    var isOverlayVisible by remember { mutableStateOf(true) }
    
    // Auto-dismiss overlay after 5 seconds of inactivity
    LaunchedEffect(isOverlayVisible, lastInteractionTime) {
        if (isOverlayVisible) {
            kotlinx.coroutines.delay(5000)
            isOverlayVisible = false
        }
    }

    val isPlayerTab = activeTab in 0..4
    val isPlaying = isPlayerTab && activePlayUrl != null
    
    // Auto-dismiss main UI menu after 5 seconds when a video is playing in player tabs
    LaunchedEffect(isUiVisible, activePlayUrl, activeTab, lastInteractionTime) {
        if (isUiVisible && activePlayUrl != null && isPlayerTab) {
            kotlinx.coroutines.delay(5000)
            isUiVisible = false
        }
    }

    // Automatically stop/clear video player and on screen overlays when switching to settings like profile and playlists screens (tabs 7, 8, 9)
    LaunchedEffect(activeTab) {
        if (activeTab in 7..9) {
            viewModel.clearSelectedChannel()
            isUiVisible = true
        }
    }


    var isFullScreenMobile by remember { mutableStateOf(false) }
    var showMobileOverlayMenu by remember { mutableStateOf(false) }
    var showMobileControls by remember { mutableStateOf(true) }

    LaunchedEffect(showMobileControls, showMobileOverlayMenu) {
        if (showMobileControls && !showMobileOverlayMenu) {
            kotlinx.coroutines.delay(8000)
            showMobileControls = false
        }
    }

    LaunchedEffect(activePlayUrl) {
        if (activePlayUrl != null) {
            isFullScreenMobile = false
            showMobileOverlayMenu = false
            showMobileControls = true
        } else {
            isFullScreenMobile = false
            showMobileOverlayMenu = false
            showMobileControls = false
        }
    }

    if (deviceMode == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07090C)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFFB03A).copy(alpha = 0.08f), Color.Transparent)
                        )
                    )
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFFFB03A).copy(alpha = 0.3f)),
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 500.dp)
                    .testTag("device_setup_card")
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CaptnHackLogo(modifier = Modifier.size(80.dp), showText = false, animate = true)

                    Text(
                        text = "WELCOME ABOARD, MATEY!",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color(0xFFFFB03A),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    )

                    Text(
                        text = "Select how you want to configure this device. You can control a TV screen directly from your mobile device!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9096A5),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = { viewModel.setDeviceMode("MOBILE") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5390F5),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("setup_mobile_button")
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = "Mobile Mode")
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Mobile / Handheld Mode", fontWeight = FontWeight.Bold)
                            Text("Touch UI + Remote Controller for TV", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    Button(
                        onClick = { viewModel.setDeviceMode("TV") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB03A),
                            contentColor = Color(0xFF131722)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("setup_tv_button")
                    ) {
                        Icon(Icons.Default.Tv, contentDescription = "TV Mode")
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("TV Screen Mode", fontWeight = FontWeight.Bold)
                            Text("Large display + Socket Receiver Active", style = MaterialTheme.typography.labelSmall, color = Color(0xFF131722).copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
        return
    }

    if (showPlaylistSelectorOnStart && playlists.size > 1) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "Select Playlist to Load",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Multiple playlists found. Please choose which playlist to load for this session:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    playlists.forEach { playlist ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.loadPlaylistOnStart(playlist.id)
                                }
                                .testTag("start_playlist_${playlist.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = "Playlist",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = playlist.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Removed device setup banner. Defaulting to MOBILE mode.
    // In a real scenario, we might detect TV vs Mobile using UiModeManager.
    // For now, we proceed as if MOBILE is the default.

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        lastInteractionTime = System.currentTimeMillis()
                        if (!isUiVisible && isPlayerTab && activePlayUrl != null) {
                            isUiVisible = true
                        }
                    }
                }
            }
    ) {
        val currentPlayUrl = activePlayUrl
        val currentChannel = selectedChannel
        
        // 1. Background Video Player for all player tabs (Live TV, PPV, Movies, Shows)
        if (isPlayerTab && currentPlayUrl != null && currentChannel != null) {
            val context = LocalContext.current
            val subLabel = when (activeTab) {
                0 -> "Live TV"
                1 -> "PPV"
                2 -> "Movies"
                3 -> "Shows"
                4 -> "Favorites"
                else -> ""
            }
            VideoPlayer(
                videoUrl = currentPlayUrl,
                title = currentChannel.name,
                subtitle = subLabel,
                onDownloadClick = { 
                    viewModel.downloadActiveStream()
                    android.widget.Toast.makeText(context, "Download started for ${currentChannel.name}", android.widget.Toast.LENGTH_SHORT).show()
                },
                isLiveStream = activeTab == 0 || activeTab == 1 || (activeTab == 4 && (currentChannel.getStreamType() == StreamType.LIVE_TV || currentChannel.getStreamType() == StreamType.PPV)),
                remoteCommands = viewModel.remoteCommandFlow,
                onPlaybackStarted = {
                    if (autoFullscreen) {
                        isFullScreenMobile = true
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isUiVisible = !isUiVisible }
            )
        }

        // 2. Main content area
        AnimatedVisibility(
            visible = isUiVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Main content area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val isPlaying = isPlayerTab && currentPlayUrl != null
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .then(if (isPlaying) Modifier.width(350.dp) else Modifier.fillMaxWidth())
                            .background(Color(0xFF0F1115).copy(alpha = if (isPlaying) 1.0f else 1.0f))
                            .align(if (isPlaying) Alignment.CenterStart else Alignment.Center)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            TabContent(
                                deviceMode = deviceMode ?: "MOBILE",
                                activeTab = activeTab,
                                viewModel = viewModel,
                                onNavigateToPlaylists = { activeTab = 6 }
                            )
                        }
                    }
                    
                    // Show overlay only if in Live TV tab (tab 0)
                    if (activeTab == 0) {
                        LiveTvOverlay(visible = isOverlayVisible)
                    }
                }
                
                // 1. Bottom Navigation Bar
                NavigationBar(
                    containerColor = Color(0xFF131722).copy(alpha = if (isPlaying) 1.0f else 1f),
                    contentColor = Color.White
                ) {
                    val deviceMode by viewModel.deviceMode.collectAsState()
                    
                    val navItems = mutableListOf(
                        "Live TV" to Icons.Default.Tv,
                        "PPV" to Icons.Default.Event,
                        "Movies" to Icons.Default.Movie,
                        "Shows" to Icons.Default.VideoLibrary,
                        "Favorites" to Icons.Default.Favorite,
                        "EPG" to Icons.Default.LiveTv,
                        "Recordings" to Icons.Default.CloudDownload,
                        "Playlists" to Icons.Default.PlaylistPlay,
                        "Profile" to Icons.Default.Person
                    )
                    
                    if (deviceMode == "TV") {
                        navItems.add("Pair" to Icons.Default.SettingsBluetooth)
                    } else {
                        navItems.add("Remote" to Icons.Default.SettingsRemote)
                    }

                    navItems.forEachIndexed { index, (label, icon) ->
                        val isSelected = activeTab == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { 
                                activeTab = index 
                                when (index) {
                                    0 -> {
                                        viewModel.selectStreamType(StreamType.LIVE_TV)
                                        viewModel.selectCategory("All")
                                    }
                                    1 -> {
                                        viewModel.selectStreamType(StreamType.PPV)
                                        viewModel.selectCategory("All")
                                    }
                                    2 -> {
                                        viewModel.selectStreamType(StreamType.MOVIE)
                                        viewModel.selectCategory("All")
                                    }
                                    3 -> {
                                        viewModel.selectStreamType(StreamType.TV_SHOW)
                                        viewModel.selectCategory("All")
                                    }
                                    4 -> {
                                        viewModel.selectCategory("Favorites")
                                    }
                                }
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            icon = { Icon(icon, contentDescription = label) }
                        )
                    }
                }
            }
        }

        // If UI is hidden in player tabs, show a menu button to bring it back and a close button
        if (isPlayerTab && currentPlayUrl != null && !isUiVisible) {
            IconButton(
                onClick = { isUiVisible = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
            }
            
            IconButton(
                onClick = { viewModel.clearSelectedChannel(); isUiVisible = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
        
        // Full screen video player overlay for other tabs (activeTab not in 0..4 and not in settings tabs)
        if (currentPlayUrl != null && currentChannel != null && !isPlayerTab && activeTab !in 7..9) {
            FullScreenPlayerOverlay(
                isInPipMode = isInPipMode,
                activePlayUrl = currentPlayUrl,
                selectedChannel = currentChannel,
                viewModel = viewModel,
                onClose = { viewModel.clearSelectedChannel() },
                onMenuClick = { viewModel.clearSelectedChannel() } // map menu click to close since no inline mode
            )
        }
    }
}

@Composable
fun FullScreenPlayerOverlay(
    isInPipMode: Boolean = false,
    activePlayUrl: String,
    selectedChannel: ChannelEntity,
    viewModel: IptvViewModel,
    onClose: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEpgOverlay by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Auto-dismiss EPG overlay after 5 seconds of inactivity
    LaunchedEffect(showEpgOverlay, lastInteractionTime) {
        if (showEpgOverlay) {
            kotlinx.coroutines.delay(5000)
            showEpgOverlay = false
        }
    }

    val streamType = selectedChannel.getStreamType()
    val isPlayingCatchup by viewModel.isPlayingCatchup.collectAsState()
    val selectedProgram by viewModel.selectedProgram.collectAsState()
    
    val subtitle = when {
        isPlayingCatchup && selectedProgram != null -> "Catchup: ${selectedProgram?.title}"
        streamType == StreamType.MOVIE -> "Movie (On Demand)"
        streamType == StreamType.TV_SHOW -> "TV Show (On Demand)"
        streamType == StreamType.PPV -> "PPV Live Event"
        else -> "Live TV Broadcast"
    }
    val isLiveStream = (streamType == StreamType.LIVE_TV || streamType == StreamType.PPV) && !isPlayingCatchup

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        lastInteractionTime = System.currentTimeMillis()
                        if (!showEpgOverlay) {
                            showEpgOverlay = true
                        }
                    }
                }
            }
    ) {
        // The Video Player
        val context = LocalContext.current
        VideoPlayer(
            videoUrl = activePlayUrl,
            title = selectedChannel.name,
            subtitle = subtitle,
            isInPipMode = isInPipMode,
            onDownloadClick = {
                viewModel.downloadActiveStream()
                android.widget.Toast.makeText(context, "Download started for ${selectedChannel.name}", android.widget.Toast.LENGTH_SHORT).show()
            },
            isLiveStream = isLiveStream,
            modifier = Modifier.fillMaxSize()
        )
        
        if (!isInPipMode) {
            // Top right close button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Player", tint = Color.White)
            }
    
            // Opaque overlay EPG on the left containing channel listings
            AnimatedVisibility(
                visible = showEpgOverlay,
                enter = slideInHorizontally() + fadeIn(),
                exit = slideOutHorizontally() + fadeOut(),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.25f) // Takes 25% of the screen width
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Channels & EPG",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    val filteredChannels by viewModel.filteredChannels.collectAsState()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredChannels) { channel ->
                            val isPlaying = channel.id == selectedChannel.id
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (isPlaying) MaterialTheme.colorScheme.primary else Color.DarkGray),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectChannel(channel)
                                        showEpgOverlay = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ChannelLogo(
                                        logoUrl = channel.logoUrl,
                                        channelName = channel.name,
                                        modifier = Modifier.size(40.dp),
                                        isPlaying = isPlaying
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = channel.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        // Current EPG program could go here
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Menu button to reopen EPG overlay if it's hidden
        if (!showEpgOverlay) {
            IconButton(
                onClick = { showEpgOverlay = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Open Channels", tint = Color.White)
            }
        }
        } // Close if (!isInPipMode)
    }
}

@Composable
fun ActiveChannelDetailsCard(
    selectedChannel: ChannelEntity?,
    isPlayingCatchup: Boolean,
    selectedProgram: EpgProgramEntity?,
    onFavoriteToggle: (ChannelEntity) -> Unit,
    onDownloadToggle: () -> Unit
) {
    if (selectedChannel != null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChannelLogo(
                    logoUrl = selectedChannel.logoUrl,
                    channelName = selectedChannel.name,
                    modifier = Modifier.size(56.dp),
                    isPlaying = true
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedChannel.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedChannel.groupTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isPlayingCatchup && selectedProgram != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Catchup Playback",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Catchup: ${selectedProgram.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Download Button
                IconButton(
                    onClick = onDownloadToggle,
                    modifier = Modifier.testTag("download_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Download Stream",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { onFavoriteToggle(selectedChannel) },
                    modifier = Modifier.testTag("favorite_button")
                ) {
                    Icon(
                        imageVector = if (selectedChannel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite Channel",
                        tint = if (selectedChannel.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardTabsHeader(
    deviceMode: String?,
    activeTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = remember(deviceMode) {
        val list = mutableListOf(
            "Live TV" to Icons.Default.Tv,
            "PPV / Events" to Icons.Default.Event,
            "Movies" to Icons.Default.Movie,
            "TV Shows" to Icons.Default.VideoLibrary,
            "Favorites" to Icons.Default.Favorite,
            "EPG Guide" to Icons.Default.LiveTv,
            "Recordings" to Icons.Default.CloudDownload,
            "Playlists" to Icons.Default.PlaylistPlay,
            "My Profile" to Icons.Default.Person
        )
        if (deviceMode == "MOBILE") {
            list.add("TV Remote" to Icons.Default.SettingsRemote)
        } else if (deviceMode == "TV") {
            list.add("TV Info" to Icons.Default.Router)
        }
        list
    }

    ScrollableTabRow(
        selectedTabIndex = activeTab,
        edgePadding = 0.dp,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {}
    ) {
        tabs.forEachIndexed { index, (label, icon) ->
            Tab(
                selected = activeTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = label,
                        fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal
                    )
                },
                icon = {
                    Icon(imageVector = icon, contentDescription = label)
                }
            )
        }
    }
}

@Composable
fun TabContent(
    deviceMode: String?,
    activeTab: Int,
    viewModel: IptvViewModel,
    onNavigateToPlaylists: () -> Unit
) {
    Crossfade(targetState = activeTab, label = "TabTransition") { tab ->
        when (tab) {
            0 -> ChannelsExplorerTab(viewModel = viewModel)
            1 -> ChannelsExplorerTab(viewModel = viewModel)
            2 -> ChannelsExplorerTab(viewModel = viewModel)
            3 -> ChannelsExplorerTab(viewModel = viewModel)
            4 -> FavoritesTab(viewModel = viewModel)
            5 -> EpgGuideTab(viewModel = viewModel)
            6 -> RecordingsTab(viewModel = viewModel)
            7 -> PlaylistsTab(viewModel = viewModel)
            8 -> ProfileTab(viewModel = viewModel, onNavigateToPlaylists = onNavigateToPlaylists)
            9 -> {
                if (deviceMode == "MOBILE") {
                    TvControllerTab(viewModel = viewModel)
                } else {
                    TvConnectionsTab(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun VodCard(channel: ChannelEntity, activeChannel: ChannelEntity?, viewModel: IptvViewModel) {
    var showInfoDialog by remember { mutableStateOf(false) }
    val isPlaying = activeChannel?.id == channel.id
    val scale by animateFloatAsState(targetValue = if (isPlaying) 1.02f else 1f, label = "card_scale")
    
    var vodInfo by remember { mutableStateOf<org.json.JSONObject?>(null) }
    var isLoadingInfo by remember { mutableStateOf(false) }

    LaunchedEffect(showInfoDialog) {
        if (showInfoDialog && vodInfo == null) {
            isLoadingInfo = true
            vodInfo = viewModel.getVodOrSeriesInfo(channel.streamUrl)
            isLoadingInfo = false
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlaying) 8.dp else 2.dp),
        border = if (isPlaying) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                shadowElevation = if (isPlaying) 16f else 4f,
                shape = RoundedCornerShape(12.dp),
                clip = true
            )
            .clickable { viewModel.selectChannel(channel) }
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                if (!channel.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                // Play button overlay
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleOutline,
                        contentDescription = "Play",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(48.dp)
                    )
                }

                val streamType = remember(channel) { channel.getStreamType().name.replace("_", " ") }
                val qualityTag = remember(channel.name) {
                    val upperName = channel.name.uppercase()
                    when {
                        upperName.contains("4K") || upperName.contains("UHD") -> "4K"
                        upperName.contains("FHD") || upperName.contains("1080") -> "FHD"
                        upperName.contains("HD") || upperName.contains("720") -> "HD"
                        upperName.contains("SD") || upperName.contains("480") -> "SD"
                        else -> ""
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = streamType,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (qualityTag.isNotEmpty()) {
                        Surface(
                            color = Color(0xFFE50914).copy(alpha = 0.8f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = qualityTag,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                IconButton(
                    onClick = { showInfoDialog = true },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                }

                IconButton(
                    onClick = { viewModel.toggleFavorite(channel) },
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite Toggle",
                        tint = if (channel.isFavorite) Color.Red else Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = channel.groupTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(text = channel.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (!channel.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                        )
                    }
                    Text("Genre: ${channel.groupTitle}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    
                    if (isLoadingInfo) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (vodInfo != null) {
                        val info = vodInfo?.optJSONObject("info")
                        if (info != null) {
                            val description = info.optString("plot", "").takeIf { it.isNotBlank() } ?: info.optString("description", "No description available.")
                            val rating = info.optString("rating", "N/A")
                            val cast = info.optString("cast", "N/A")
                            val director = info.optString("director", "N/A")
                            val releaseDate = info.optString("release_date", "N/A")
                            
                            Text("Rating: $rating", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Release Date: $releaseDate", style = MaterialTheme.typography.bodySmall)
                            Text("Director: $director", style = MaterialTheme.typography.bodySmall)
                            Text("Cast: $cast", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Plot:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(description, style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Additional metadata is not available for this item.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text("Failed to load API info.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { 
                    showInfoDialog = false
                    viewModel.selectChannel(channel)
                }) {
                    Text("Play Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ChannelsExplorerTab(viewModel: IptvViewModel) {
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val channels by viewModel.filteredChannels.collectAsState()
    val activeChannel by viewModel.selectedChannel.collectAsState()
    val deviceMode by viewModel.deviceMode.collectAsState()
    val isTvConnected by viewModel.isTvConnected.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val countries by viewModel.countries.collectAsState()

    var showCategorySidebarMobile by remember { mutableStateOf(false) }

    // Auto-dismiss sidebar after 8 seconds of inactivity
    LaunchedEffect(showCategorySidebarMobile) {
        if (showCategorySidebarMobile) {
            kotlinx.coroutines.delay(8000)
            showCategorySidebarMobile = false
        }
    }

    val selectedStreamType by viewModel.selectedStreamType.collectAsState()
    val isLiveOrPpv = selectedStreamType == StreamType.LIVE_TV || selectedStreamType == StreamType.PPV

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLiveOrPpv) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Permanent sidebar on the left side of the screen
                com.example.ui.components.CategorySidebar(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        viewModel.selectCategory(category)
                    },
                    categoryCounts = categoryCounts,
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Right-aligned channel grid layout
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search channel...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("channel_search_bar")
                        )

                        // Country selector dropdown trigger
                        var countryMenuExpanded by remember { mutableStateOf(false) }

                        Box {
                            Button(
                                onClick = { countryMenuExpanded = true },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedCountry == "All") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                                    contentColor = if (selectedCountry == "All") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("country_selector_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = "Country",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (selectedCountry == "All") "Country" else selectedCountry,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            DropdownMenu(
                                expanded = countryMenuExpanded,
                                onDismissRequest = { countryMenuExpanded = false }
                            ) {
                                countries.forEach { cry ->
                                    DropdownMenuItem(
                                        text = { Text(cry) },
                                        onClick = {
                                            viewModel.selectCountry(cry)
                                            countryMenuExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Public,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        modifier = Modifier.testTag("country_option_$cry")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Grid/List of Channels
                    if (channels.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.TvOff,
                                    contentDescription = "No channels found",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No channels match your search" else "No channels in this category",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        if (searchQuery.isNotEmpty()) {
                            val chunkedChannels = remember(channels) { channels.chunked(3) }
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .testTag("channel_list")
                            ) {
                                items(chunkedChannels) { rowChannels ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        rowChannels.forEach { channel ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                NetflixPosterCard(
                                                    channel = channel,
                                                    activeChannel = activeChannel,
                                                    viewModel = viewModel
                                                )
                                            }
                                        }
                                        val emptySpaces = 3 - rowChannels.size
                                        repeat(emptySpaces) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                NetflixDashboard(
                                    channels = channels,
                                    activeChannel = activeChannel,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search bar with Categories drawer trigger icon
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search channel...") },
                        leadingIcon = {
                            IconButton(
                                onClick = { showCategorySidebarMobile = true },
                                modifier = Modifier.testTag("open_categories_sidebar_mobile")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = "Open Categories Menu",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("channel_search_bar")
                    )

                    // Country selector dropdown trigger
                    var countryMenuExpanded by remember { mutableStateOf(false) }

                    Box {
                        Button(
                            onClick = { countryMenuExpanded = true },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedCountry == "All") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                                contentColor = if (selectedCountry == "All") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("country_selector_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Country",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedCountry == "All") "Country" else selectedCountry,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        DropdownMenu(
                            expanded = countryMenuExpanded,
                            onDismissRequest = { countryMenuExpanded = false }
                        ) {
                            countries.forEach { cry ->
                                DropdownMenuItem(
                                    text = { Text(cry) },
                                    onClick = {
                                        viewModel.selectCountry(cry)
                                        countryMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Public,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("country_option_$cry")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Horizontal Category slide as fallback / quick access
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Grid/List of Channels
                if (channels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.TvOff,
                                contentDescription = "No channels found",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No channels match your search" else "No channels in this category",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    if (searchQuery.isNotEmpty()) {
                        // Show search results in a clean, high-performance responsive grid of posters
                        val chunkedChannels = remember(channels) { channels.chunked(3) }
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("channel_list")
                        ) {
                            items(chunkedChannels) { rowChannels ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowChannels.forEach { channel ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            NetflixPosterCard(
                                                channel = channel,
                                                activeChannel = activeChannel,
                                                viewModel = viewModel
                                            )
                                        }
                                    }
                                    val emptySpaces = 3 - rowChannels.size
                                    repeat(emptySpaces) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        // Immersive Netflix Dashboard when no active search
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            NetflixDashboard(
                                channels = channels,
                                activeChannel = activeChannel,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            // Semitransparent Scrim Overlay when Category Sidebar is open
            if (showCategorySidebarMobile) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showCategorySidebarMobile = false }
                        .testTag("category_sidebar_scrim_mobile")
                )
            }

            // Sliding left CategorySidebar component
            AnimatedVisibility(
                visible = showCategorySidebarMobile,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it }),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(260.dp)
                    .align(Alignment.CenterStart)
            ) {
                com.example.ui.components.CategorySidebar(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        viewModel.selectCategory(category)
                        showCategorySidebarMobile = false
                    },
                    categoryCounts = categoryCounts,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun FavoritesTab(viewModel: IptvViewModel) {
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    val activeChannel by viewModel.selectedChannel.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredFavorites = remember(favoriteChannels, searchQuery) {
        if (searchQuery.isBlank()) {
            favoriteChannels
        } else {
            favoriteChannels.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Favorites",
                tint = Color(0xFFE53935),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "My Favorites",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // Search Bar (if favorites is not empty)
        if (favoriteChannels.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search through your favorites...", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("favorites_search_bar")
            )
        }

        if (filteredFavorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "No Favorites",
                        tint = Color(0xFFE53935).copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching favorites found" else "Your Favorites list is empty",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try searching for a different channel name." else "To add a channel to your favorites, click the Heart icon while playing a channel or browsing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.widthIn(max = 320.dp)
                    )
                }
            }
        } else {
            val chunkedChannels = remember(filteredFavorites) { filteredFavorites.chunked(3) }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("favorites_list")
            ) {
                items(chunkedChannels) { rowChannels ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowChannels.forEach { channel ->
                            Box(modifier = Modifier.weight(1f)) {
                                NetflixPosterCard(
                                    channel = channel,
                                    activeChannel = activeChannel,
                                    viewModel = viewModel
                                )
                            }
                        }
                        val emptySpaces = 3 - rowChannels.size
                        repeat(emptySpaces) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpgGuideTab(viewModel: IptvViewModel) {
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val epgPrograms by viewModel.epgPrograms.collectAsState()
    val activePlayUrl by viewModel.activePlayUrl.collectAsState()
    val isPlayingCatchup by viewModel.isPlayingCatchup.collectAsState()

    if (selectedChannel == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a channel to view program guide.")
        }
        return
    }

    val now = remember { System.currentTimeMillis() }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Programs for ${selectedChannel?.name}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("epg_list")
        ) {
            items(epgPrograms) { program ->
                val isPast = program.endTime <= now
                val isFuture = program.startTime > now
                val isCurrent = now in program.startTime..program.endTime

                val timeStr = "${timeFormat.format(Date(program.startTime))} - ${timeFormat.format(Date(program.endTime))}"
                val dateStr = dateFormat.format(Date(program.startTime))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isPast || isCurrent) {
                            if (isPast) {
                                viewModel.playCatchupProgram(program)
                            } else if (isCurrent) {
                                selectedChannel?.let { viewModel.selectChannel(it) }
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = program.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (!program.description.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = program.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Status Badge & Action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when {
                                isPast -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "Catchup",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "CATCH-UP AVAILABLE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                isCurrent -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = "Live",
                                            tint = Color.Red,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "LIVE NOW",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Red,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                isFuture -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "Upcoming",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "UPCOMING",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            
                            val context = LocalContext.current
                            if (isCurrent || isFuture) {
                                IconButton(
                                    onClick = {
                                        if (isCurrent) {
                                            viewModel.addCustomUrlDownload(program.title, selectedChannel!!.streamUrl)
                                            android.widget.Toast.makeText(context, "Recording started for ${program.title}", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.scheduleRecording(program.title, selectedChannel!!.streamUrl)
                                            android.widget.Toast.makeText(context, "Recording scheduled for ${program.title}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCurrent) Icons.Default.FiberManualRecord else Icons.Default.AlarmAdd,
                                        contentDescription = if (isCurrent) "Record" else "Schedule",
                                        tint = if (isCurrent) Color.Red else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Live Program Slider Progress
                        if (isCurrent) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val totalDuration = program.endTime - program.startTime
                            val elapsed = now - program.startTime
                            val progress = (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = progress,
                                color = Color.Red,
                                trackColor = Color.Red.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingsTab(viewModel: IptvViewModel) {
    val downloads by viewModel.downloads.collectAsState()
    val activeStates by viewModel.activeDownloadStates.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Active", "Completed", "Paused", "Failed"
    var showAddDialog by remember { mutableStateOf(false) }

    // Dialog state for custom link download
    var customName by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }

    // Disk storage calculation
    val filesDir = context.filesDir
    val totalSpace = filesDir.totalSpace
    val usableSpace = filesDir.usableSpace
    val usedSpace = totalSpace - usableSpace
    val usedPercentage = if (totalSpace > 0) (usedSpace.toFloat() / totalSpace) else 0f

    // Helper functions for units
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun formatSpeed(speedBps: Long): String {
        if (speedBps <= 0) return "0 B/s"
        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
        val digitGroups = (Math.log10(speedBps.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.US, "%.1f %s", speedBps / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun formatEta(seconds: Long): String {
        if (seconds < 0) return "Unknown"
        if (seconds == 0L) return "0s"
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    // Filter downloads list
    val filteredDownloads = downloads.filter { download ->
        val activeState = activeStates[download.id]
        val currentStatus = activeState?.status ?: download.status

        // Search name
        val matchesSearch = download.channelName.contains(searchQuery, ignoreCase = true) ||
                download.streamUrl.contains(searchQuery, ignoreCase = true)

        // Filter status
        val matchesStatus = when (selectedFilter) {
            "All" -> true
            "Active" -> currentStatus == "DOWNLOADING" || currentStatus == "QUEUED"
            "Completed" -> currentStatus == "COMPLETED"
            "Paused" -> currentStatus == "PAUSED"
            "Failed" -> currentStatus == "FAILED"
            else -> true
        }

        matchesSearch && matchesStatus
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "VOD & Stream Downloader",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color(0xFFFFB03A),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Manage your offline VOD collection & recordings",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9096A5)
                )
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB03A),
                    contentColor = Color(0xFF131722)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Custom Link", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add URL", fontWeight = FontWeight.Bold)
            }
        }

        // Storage Usage Panel Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF232B3E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Storage Status",
                            tint = Color(0xFFFFB03A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Storage & Local Space",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "${formatBytes(usableSpace)} free of ${formatBytes(totalSpace)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFB03A),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { usedPercentage },
                    color = Color(0xFFFFB03A),
                    trackColor = Color(0xFF232B3E),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Used Space: ${formatBytes(usedSpace)} (${(usedPercentage * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9096A5)
                    )
                    Text(
                        text = "VOD folder: App Local Directory",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9096A5)
                    )
                }
            }
        }

        // Search & Filter Box
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by file name or URL...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFB03A),
                    unfocusedBorderColor = Color(0xFF232B3E),
                    focusedLabelColor = Color(0xFFFFB03A),
                    cursorColor = Color(0xFFFFB03A),
                    unfocusedContainerColor = Color(0xFF0F121C),
                    focusedContainerColor = Color(0xFF0F121C)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
        }

        // Filter chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("All", "Active", "Completed", "Paused", "Failed")
            filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFB03A).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFFFFB03A),
                        selectedLeadingIconColor = Color(0xFFFFB03A),
                        containerColor = Color(0xFF131722),
                        labelColor = Color(0xFF9096A5)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) Color(0xFFFFB03A) else Color(0xFF232B3E)
                    )
                )
            }
        }

        // Downloads List
        if (filteredDownloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "No files found",
                        tint = Color(0xFF232B3E),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty() || selectedFilter != "All") "No matching downloads found" else "No downloads or recordings yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty() || selectedFilter != "All") "Try modifying your search or filters" else "Press the Download button on any VOD stream to start recording!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9096A5),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredDownloads, key = { it.id }) { download ->
                    val activeState = activeStates[download.id]
                    val isDownloading = activeState != null && activeState.status == "DOWNLOADING"
                    val currentProgress = activeState?.progress ?: download.progress
                    val currentStatus = activeState?.status ?: download.status

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isDownloading) Color(0xFFFFB03A).copy(alpha = 0.5f) else Color(0xFF232B3E)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Title & Badge Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = download.channelName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = download.streamUrl,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF9096A5),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                // Status Badge
                                val badgeColor = when (currentStatus) {
                                    "COMPLETED" -> Color(0xFF4CAF50)
                                    "DOWNLOADING" -> Color(0xFF2196F3)
                                    "PAUSED" -> Color(0xFFFF9800)
                                    "QUEUED" -> Color(0xFF9E9E9E)
                                    "FAILED" -> Color(0xFFF44336)
                                    else -> Color.Gray
                                }
                                Box(
                                    modifier = Modifier
                                        .background(badgeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                        .border(1.dp, badgeColor.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = currentStatus,
                                        color = badgeColor,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Progress bar
                            LinearProgressIndicator(
                                progress = { currentProgress },
                                color = when (currentStatus) {
                                    "COMPLETED" -> Color(0xFF4CAF50)
                                    "FAILED" -> Color(0xFFF44336)
                                    "PAUSED" -> Color(0xFFFF9800)
                                    else -> Color(0xFFFFB03A)
                                },
                                trackColor = Color(0xFF232B3E),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Speeds, Sizes, ETA details Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left details: size / totals
                                Row {
                                    if (currentStatus == "COMPLETED") {
                                        Text(
                                            text = "Size: ${formatBytes(download.fileSize)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF9096A5)
                                        )
                                    } else {
                                        val downloadedStr = formatBytes(activeState?.downloadedBytes ?: download.fileSize)
                                        val totalStr = if (activeState != null && activeState.totalBytes > 0) formatBytes(activeState.totalBytes) else "unknown"
                                        Text(
                                            text = "$downloadedStr / $totalStr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF9096A5)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "${(currentProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB03A)
                                    )
                                }

                                // Right details: Speed and ETA
                                if (isDownloading && activeState != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatSpeed(activeState.speedBps),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF2196F3),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "ETA: ${formatEta(activeState.etaSeconds)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF9096A5)
                                        )
                                    }
                                } else {
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(download.downloadTime))
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF555D6F)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Actions Bar Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when (currentStatus) {
                                    "COMPLETED" -> {
                                        Button(
                                            onClick = {
                                                viewModel.selectChannel(
                                                    ChannelEntity(
                                                        id = -999,
                                                        playlistId = -1,
                                                        name = download.channelName,
                                                        logoUrl = null,
                                                        groupTitle = "Local Recording",
                                                        streamUrl = download.filePath,
                                                        tvgId = null,
                                                        tvgName = null,
                                                        catchupType = null,
                                                        catchupSource = null
                                                    )
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB03A)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(16.dp), tint = Color(0xFF131722))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Play Local", fontWeight = FontWeight.Bold, color = Color(0xFF131722))
                                        }
                                    }
                                    "DOWNLOADING" -> {
                                        Button(
                                            onClick = { viewModel.pauseDownload(download.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232B3E)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(16.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Pause", color = Color.White)
                                        }

                                        Button(
                                            onClick = { viewModel.cancelDownload(download.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f)),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text("Cancel", color = Color.Red, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    "PAUSED" -> {
                                        Button(
                                            onClick = { viewModel.startDownload(download.id, download.channelName, download.streamUrl, download.filePath) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB03A)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(16.dp), tint = Color(0xFF131722))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Resume", fontWeight = FontWeight.Bold, color = Color(0xFF131722))
                                        }

                                        Button(
                                            onClick = { viewModel.cancelDownload(download.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(0xFF232B3E)),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text("Delete Record", color = Color(0xFF9096A5))
                                        }
                                    }
                                    "FAILED" -> {
                                        Button(
                                            onClick = { viewModel.startDownload(download.id, download.channelName, download.streamUrl, download.filePath) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB03A)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp), tint = Color(0xFF131722))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Retry", fontWeight = FontWeight.Bold, color = Color(0xFF131722))
                                        }
                                    }
                                }

                                if (currentStatus != "DOWNLOADING") {
                                    IconButton(
                                        onClick = { viewModel.deleteDownload(download) },
                                        modifier = Modifier.testTag("delete_download_button_${download.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Download",
                                            tint = Color.Red.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom URL Downloader Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Download VOD from Stream URL",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Paste a direct stream link (MP4, MKV, AVI, TS, etc.) to start a resumeable background download.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9096A5)
                    )

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Display Name") },
                        placeholder = { Text("e.g. My Custom VOD Video") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFB03A),
                            unfocusedBorderColor = Color(0xFF232B3E),
                            focusedLabelColor = Color(0xFFFFB03A),
                            cursorColor = Color(0xFFFFB03A)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        label = { Text("Direct Stream URL") },
                        placeholder = { Text("e.g. https://domain.com/movie.mp4") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFB03A),
                            unfocusedBorderColor = Color(0xFF232B3E),
                            focusedLabelColor = Color(0xFFFFB03A),
                            cursorColor = Color(0xFFFFB03A)
                        ),
                        singleLine = false,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customUrl.isNotEmpty()) {
                            val name = if (customName.isEmpty()) "Custom Link VOD" else customName
                            viewModel.addCustomUrlDownload(name, customUrl)
                            // Reset state
                            customName = ""
                            customUrl = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB03A),
                        contentColor = Color(0xFF131722)
                    )
                ) {
                    Text("Start Download", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        customName = ""
                        customUrl = ""
                        showAddDialog = false
                    }
                ) {
                    Text("Cancel", color = Color(0xFF9096A5))
                }
            },
            containerColor = Color(0xFF131722),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun PlaylistsTab(viewModel: IptvViewModel) {
    val playlists by viewModel.playlists.collectAsState()
    val isAdding by viewModel.isAddingPlaylist.collectAsState()
    val errorMsg by viewModel.playlistAddError.collectAsState()

    var isXtreamMode by remember { mutableStateOf(false) }
    var showAddForm by remember { mutableStateOf(false) }

    // M3U form state
    var playlistName by remember { mutableStateOf("") }
    var playlistUrl by remember { mutableStateOf("") }

    // Xtream Codes form state
    var xtreamName by remember { mutableStateOf("") }
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }

    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            playlistUrl = uri.toString()
            if (playlistName.isEmpty()) {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                val nameIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                var displayName = ""
                if (cursor != null && nameIndex != null && nameIndex != -1 && cursor.moveToFirst()) {
                    displayName = cursor.getString(nameIndex)
                }
                cursor?.close()
                playlistName = if (displayName.isNotEmpty()) {
                    displayName.substringBeforeLast(".")
                } else {
                    "Local M3U File"
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Playlists",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { showAddForm = !showAddForm }
            ) {
                Icon(if (showAddForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (showAddForm) "Cancel" else "Add Playlist")
            }
        }

        if (showAddForm) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Tab-like FilterChips to choose between M3U and Xtream Codes API
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isXtreamMode,
                            onClick = { isXtreamMode = false },
                            label = { Text("M3U URL / File") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = isXtreamMode,
                            onClick = { isXtreamMode = true },
                            label = { Text("Xtream API") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    if (!isXtreamMode) {
                        OutlinedTextField(
                            value = playlistName,
                            onValueChange = { playlistName = it },
                            label = { Text("Playlist Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("playlist_name_field")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = playlistUrl,
                            onValueChange = { playlistUrl = it },
                            label = { Text("M3U Playlist URL or File URI") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Pick Local File", tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("playlist_url_field")
                        )
                    } else {
                        OutlinedTextField(
                            value = xtreamName,
                            onValueChange = { xtreamName = it },
                            label = { Text("Server Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = xtreamServer,
                            onValueChange = { xtreamServer = it },
                            label = { Text("Server URL (e.g. http://iptv.com:8080)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = xtreamUser,
                                onValueChange = { xtreamUser = it },
                                label = { Text("Username") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = xtreamPass,
                                onValueChange = { xtreamPass = it },
                                label = { Text("Password") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (!isXtreamMode) {
                                if (playlistName.isNotEmpty() && playlistUrl.isNotEmpty()) {
                                    viewModel.addPlaylist(playlistName, playlistUrl)
                                    playlistName = ""
                                    playlistUrl = ""
                                    showAddForm = false
                                }
                            } else {
                                if (xtreamName.isNotEmpty() && xtreamServer.isNotEmpty() && xtreamUser.isNotEmpty() && xtreamPass.isNotEmpty()) {
                                    viewModel.addXtreamPlaylist(xtreamName, xtreamServer, xtreamUser, xtreamPass)
                                    xtreamName = ""
                                    xtreamServer = ""
                                    xtreamUser = ""
                                    xtreamPass = ""
                                    showAddForm = false
                                }
                            }
                        },
                        enabled = !isAdding && (
                            (!isXtreamMode && playlistName.isNotEmpty() && playlistUrl.isNotEmpty()) ||
                            (isXtreamMode && xtreamName.isNotEmpty() && xtreamServer.isNotEmpty() && xtreamUser.isNotEmpty() && xtreamPass.isNotEmpty())
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("add_playlist_button")
                    ) {
                        if (isAdding) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing IPTV Metadata...")
                        } else {
                            Text(if (isXtreamMode) "Connect & Sync Xtream API" else "Add M3U Playlist")
                        }
                    }
                }
            }
        }

        // Active Playlists List
        if (playlists.isEmpty() && !showAddForm) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No playlists added yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("playlists_list")
            ) {
                items(playlists) { playlist ->
                    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsState()
                    val isActive = selectedPlaylistId == playlist.id
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.selectPlaylist(playlist.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        SuggestionChip(onClick = {}, label = { Text("Active") })
                                    }
                                }
                                Text(
                                    text = playlist.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                Text(
                                    text = "Synced: ${sdf.format(java.util.Date(playlist.lastUpdated))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row {
                                IconButton(onClick = { viewModel.refreshPlaylist(playlist.id) }) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Playlist", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Playlist", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileTab(viewModel: IptvViewModel, onNavigateToPlaylists: () -> Unit) {
    val username by viewModel.premiumUsername.collectAsState()
    val password by viewModel.premiumPassword.collectAsState()
    val serverUrl by viewModel.premiumServerUrl.collectAsState()
    val status by viewModel.premiumStatus.collectAsState()
    val expiry by viewModel.premiumExpiry.collectAsState()
    val maxConn by viewModel.premiumMaxConnections.collectAsState()
    val activeConn by viewModel.premiumActiveConnections.collectAsState()
    val type by viewModel.premiumType.collectAsState()

    // Preferences states
    val avatarUrl by viewModel.avatarUrl.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val autoplayNext by viewModel.autoplayNext.collectAsState()
    val autoFullscreen by viewModel.autoFullscreen.collectAsState()
    val showTmdb by viewModel.showTmdb.collectAsState()

    // All channels for stats
    val channels by viewModel.allChannels.collectAsState()
    val liveCount = remember(channels) { channels.count { it.getStreamType() == StreamType.LIVE_TV } }
    val moviesCount = remember(channels) { channels.count { it.getStreamType() == StreamType.MOVIE } }
    val seriesCount = remember(channels) { channels.count { it.getStreamType() == StreamType.TV_SHOW } }

    var localAvatarInput by remember { mutableStateOf(avatarUrl) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER
        Column {
            Text(
                text = "PROFILE / ACCOUNT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
            Text(
                text = "SECURE LOCAL STORAGE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // USER DETAILS CARD & STATS ROW
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                val isWide = maxWidth > 480.dp

                @Composable
                fun UserInfoBlock(modifier: Modifier = Modifier) {
                    Row(
                        modifier = modifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Avatar circle with Green status dot
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = "User Avatar",
                                        modifier = Modifier.size(72.dp).background(Color.Transparent, CircleShape)
                                    )
                                } else {
                                    val initials = if (username.isNotEmpty()) username.take(2).uppercase() else "GC"
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            // Active indicator
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                                    .border(2.dp, Color(0xFF0F1115), CircleShape)
                            )
                        }

                        // Info details
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (username.isNotEmpty()) username else "Guest Captain",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (serverUrl.isNotEmpty()) serverUrl.substringBefore(":") else "guest@local",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                val badgeText = if (username.isNotEmpty()) "GOLD MEMBER" else "GUEST"
                                val badgeColor = if (username.isNotEmpty()) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                                Box(
                                    modifier = Modifier
                                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (username.isNotEmpty()) "Paid Account" else "Public Channels Only",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                @Composable
                fun UserStatsBlock(modifier: Modifier = Modifier) {
                    Row(
                        modifier = modifier.fillMaxWidth(),
                        horizontalArrangement = if (isWide) Arrangement.End else Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val stats = listOf(
                            Triple(if (liveCount > 0) liveCount.toString() else "11,902", "LIVE", Color(0xFFFFB03A)),
                            Triple(if (moviesCount > 0) moviesCount.toString() else "595", "MOVIES", Color(0xFFFFB03A)),
                            Triple(if (seriesCount > 0) seriesCount.toString() else "371", "SERIES", Color(0xFFFFB03A))
                        )
                        stats.forEachIndexed { idx, (count, label, color) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = count,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = color
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (idx < 2 && isWide) {
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }
                    }
                }

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        UserInfoBlock(modifier = Modifier.weight(1.2f))
                        UserStatsBlock(modifier = Modifier.weight(0.8f))
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        UserInfoBlock()
                        Spacer(modifier = Modifier.height(16.dp))
                        UserStatsBlock()
                    }
                }
            }
        }

        // IPTV SUBSCRIPTION BANNER
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardAlt,
                    contentDescription = "Add subscription",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Add paid subscription",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Add your own Xtream code or M3U URL from any paid provider to unlock thousands of HD/4K channels, movies and series.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onNavigateToPlaylists,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add pay service", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // PREFERENCES SUBGRID (AVATAR, DEVICE MODE, THEME, PLAYBACK PREFS)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWide = maxWidth > 600.dp
            val column1 = @Composable {
                // Column 1: AVATAR & INTERFACE MODE
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar config card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Avatar URL",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = localAvatarInput,
                                    onValueChange = { localAvatarInput = it },
                                    placeholder = { Text("Paste image URL...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        viewModel.setAvatarUrl(localAvatarInput)
                                        android.widget.Toast.makeText(context, "Avatar URL updated!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Apply")
                                }
                            }
                        }
                    }

                    // Interface Mode
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Interface Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val deviceMode by viewModel.deviceMode.collectAsState()
                            Text(
                                text = "Current: ${deviceMode ?: "Not Set"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Button(
                                onClick = { viewModel.clearDeviceMode() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                            ) {
                                Text("Reset Device Mode Selection")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    onClick = { viewModel.setDeviceMode("MOBILE") },
                                    border = BorderStroke(1.5.dp, if (deviceMode == "MOBILE") MaterialTheme.colorScheme.primary else Color.Transparent),
                                    colors = CardDefaults.cardColors(containerColor = if (deviceMode == "MOBILE") MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = if (deviceMode == "MOBILE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Mobile", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Card(
                                    onClick = { viewModel.setDeviceMode("TV") },
                                    border = BorderStroke(1.5.dp, if (deviceMode == "TV") MaterialTheme.colorScheme.primary else Color.Transparent),
                                    colors = CardDefaults.cardColors(containerColor = if (deviceMode == "TV") MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Tv, contentDescription = null, tint = if (deviceMode == "TV") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("TV Mode", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

            }
            val column2 = @Composable {
                // Column 2: THEME & PLAYBACK PREFERENCES
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Theme config card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Theme Color",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val themes = listOf(
                                "Gold" to Color(0xFFFFB03A),
                                "Ocean Blue" to Color(0xFF5390F5),
                                "Sail Red" to Color(0xFFEF5350),
                                "Emerald" to Color(0xFF10B981)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                themes.forEach { (themeName, color) ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(color, CircleShape)
                                            .border(
                                                width = if (appTheme == themeName) 2.dp else 0.dp,
                                                color = Color.White,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                viewModel.setAppTheme(themeName)
                                            }
                                    )
                                }
                            }
                        }
                    }

                    // Playback settings card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Playback Preferences (Visible)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Auto-play next episode", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = autoplayNext,
                                    onCheckedChange = { viewModel.setAutoplayNext(it) }
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Auto-enter fullscreen", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = autoFullscreen,
                                    onCheckedChange = { viewModel.setAutoFullscreen(it) }
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Show TMDB info before playing", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = showTmdb,
                                    onCheckedChange = { viewModel.setShowTmdb(it) }
                                )
                            }

                            // Sleep Timer Row
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                            Text(
                                "Sleep Timer",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val sleepTimeLeft by viewModel.sleepTimerTimeLeft.collectAsState()
                            if (sleepTimeLeft != null) {
                                val minutes = sleepTimeLeft!! / 60
                                val seconds = sleepTimeLeft!! % 60
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Active: ${minutes}m ${seconds}s remaining",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    TextButton(
                                        onClick = { viewModel.cancelSleepTimer() },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "Select timer duration to auto-stop playback:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf(5, 15, 30, 60).forEach { mins ->
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.startSleepTimer(mins)
                                                    android.widget.Toast.makeText(context, "Sleep timer set for $mins minutes", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                            ) {
                                                Text("${mins}m", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) { column1() }
                    Box(modifier = Modifier.weight(1f)) { column2() }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    column1()
                    Spacer(modifier = Modifier.height(16.dp))
                    column2()
                }
            }
        }
    }
}

@Composable
fun ChannelLogo(
    logoUrl: String?,
    channelName: String,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false
) {
    val rotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPlaying) -5f else 0f,
        label = "rotation"
    )
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPlaying) 1.15f else 1.0f,
        label = "scale"
    )
    
    val tiltModifier = modifier
        .graphicsLayer(
            rotationZ = rotation,
            scaleX = scale,
            scaleY = scale
        )

    if (!logoUrl.isNullOrEmpty()) {
        AsyncImage(
            model = logoUrl,
            contentDescription = "$channelName logo",
            contentScale = ContentScale.Crop,
            modifier = tiltModifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.1f)),
            onError = { /* Fail to initials fallback */ }
        )
    } else {
        // High-end initials fallback logo
        val initials = channelName.trim()
            .split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")

        Box(
            modifier = tiltModifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (initials.isNotEmpty()) initials else "?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun TvControllerTab(viewModel: IptvViewModel) {
    val tvIpAddress by viewModel.tvIpAddress.collectAsState()
    val isTvConnected by viewModel.isTvConnected.collectAsState()
    var ipInput by remember { mutableStateOf(tvIpAddress) }
    var pinInput by remember { mutableStateOf("") }
    var isPairing by remember { mutableStateOf(false) }
    var showManualIp by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP LOGO HEADER
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "CAP'TN HACK",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
            Text(
                text = "TV REMOTE CONTROLLER",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (!isTvConnected) {
            // Screen 2: PAIR WITH TV
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "PAIR WITH TV",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "On your TV, open Cap'tn Hack -> Account -> Pair Mode (or TV Info tab), then enter the 6-digit code shown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Enter 6-Digit PIN") },
                        placeholder = { Text("123456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(200.dp).testTag("remote_pin_field")
                    )

                    Button(
                        onClick = {
                            if (pinInput.length == 6) {
                                isPairing = true
                                viewModel.pairWithTv(pinInput) { success ->
                                    isPairing = false
                                    if (success) {
                                        android.widget.Toast.makeText(context, "TV Connected successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Pairing failed. Check PIN or ensure TV is on the same Wi-Fi.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Please enter a 6-digit PIN", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("remote_pair_button"),
                        enabled = !isPairing && pinInput.length == 6
                    ) {
                        if (isPairing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Pair with TV", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            // Divider OR Connect Manually
            TextButton(
                onClick = { showManualIp = !showManualIp }
            ) {
                Text(if (showManualIp) "Hide Manual Connection" else "Connect manually via IP Address instead", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (showManualIp) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ipInput,
                            onValueChange = { ipInput = it },
                            label = { Text("TV IP Address") },
                            placeholder = { Text("e.g. 192.168.1.100") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("remote_ip_field")
                        )

                        Button(
                            onClick = {
                                if (ipInput.isNotEmpty()) {
                                    viewModel.connectToTv(ipInput) { success ->
                                        if (success) {
                                            android.widget.Toast.makeText(context, "Connected manually!", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Manual connection failed. Is the IP address correct?", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(56.dp).testTag("remote_connect_button")
                        ) {
                            Text("Connect")
                        }
                    }
                }
            }
        } else {
            // TV IS CONNECTED: Show Remote controller pad
            Text(
                text = "Connected to TV • Send Remote Commands Below",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Arrow Keys Layout
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.width(220.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.sendRemoteCommand("UP") },
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .testTag("remote_up")
                        ) {
                            Icon(Icons.Default.ArrowDropUp, contentDescription = "Up", modifier = Modifier.size(36.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.sendRemoteCommand("LEFT") },
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    .testTag("remote_left")
                            ) {
                                Icon(Icons.Default.ArrowLeft, contentDescription = "Left", modifier = Modifier.size(36.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable { viewModel.sendRemoteCommand("PLAY_FEED") }
                                    .testTag("remote_ok"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("OK", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            }

                            IconButton(
                                onClick = { viewModel.sendRemoteCommand("RIGHT") },
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    .testTag("remote_right")
                            ) {
                                Icon(Icons.Default.ArrowRight, contentDescription = "Right", modifier = Modifier.size(36.dp))
                            }
                        }

                        IconButton(
                            onClick = { viewModel.sendRemoteCommand("DOWN") },
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .testTag("remote_down")
                        ) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Down", modifier = Modifier.size(36.dp))
                        }
                    }

                    // Media Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.sendRemoteCommand("REWIND") },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.FastRewind, contentDescription = "Rewind", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        IconButton(
                            onClick = { viewModel.sendRemoteCommand("PLAY_PAUSE") },
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                        }

                        IconButton(
                            onClick = { viewModel.sendRemoteCommand("FORWARD") },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.FastForward, contentDescription = "Fast Forward", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Volume & System Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.sendRemoteCommand("VOLUME_DOWN") },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.VolumeDown, contentDescription = "Volume Down", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        IconButton(
                            onClick = { viewModel.sendRemoteCommand("MUTE") },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.VolumeMute, contentDescription = "Mute", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        IconButton(
                            onClick = { viewModel.sendRemoteCommand("VOLUME_UP") },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Volume Up", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // System Navigation & Utilities Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.sendRemoteCommand("BACK") },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        IconButton(
                            onClick = { viewModel.sendRemoteCommand("FULLSCREEN") },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.sendRemoteCommand("TOGGLE_FAVORITE") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            modifier = Modifier.weight(1f).testTag("remote_fav")
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Favorite")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fav")
                        }

                        Button(
                            onClick = { viewModel.sendRemoteCommand("DOWNLOAD_ACTIVE") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5390F5)),
                            modifier = Modifier.weight(1f).testTag("remote_record")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Record")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Record")
                        }
                    }

                    var tvSearchInput by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = tvSearchInput,
                        onValueChange = { 
                            tvSearchInput = it
                            viewModel.sendRemoteCommand("SEARCH|$it")
                        },
                        label = { Text("Search channels on TV") },
                        placeholder = { Text("Type query to filter TV channels") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("remote_search_field")
                    )

                    Button(
                        onClick = { viewModel.disconnectFromTv() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disconnect from TV")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { viewModel.clearDeviceMode() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().testTag("remote_reset_button")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset Device Mode (Show Setup Screen)")
        }
    }
}



@Composable
fun LiveTvOverlay(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.25f)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp)
        ) {
            Text("EPG / Channels", color = Color.White)
        }
    }
}

@Composable
fun TvConnectionsTab(viewModel: IptvViewModel) {
    val pairingCode by viewModel.pairingCode.collectAsState()
    val isTvConnected by viewModel.isTvConnected.collectAsState()
    val localIp = viewModel.getLocalIpAddress()

    LaunchedEffect(Unit) {
        if (pairingCode.isEmpty()) {
            viewModel.generatePairingCode()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screen 3: ENTER THIS CODE ON YOUR PHONE
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "CAP'TN HACK",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
            Text(
                text = "PAIR THIS TV SCREEN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Server Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Server: ${localIp.ifEmpty { "Searching..." }}", style = MaterialTheme.typography.bodyMedium)
                }

                Text(
                    text = "ENTER THIS CODE ON YOUR PHONE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Large Monospaced Pairing PIN
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (pairingCode.isNotEmpty()) pairingCode else "------",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            letterSpacing = 8.sp
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Wi-Fi or Scanning status animation indicator
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isTvConnected) "Pairing Connected!" else "Waiting for phone connection...",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isTvConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                Text(
                    text = "To pair, make sure both devices are on the same local Wi-Fi. Open Cap'tn Hack on your mobile phone, navigate to 'TV Remote', then type the 6-digit code above.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TV Local IP: $localIp",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Listening Port: 9999",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.setDeviceMode("MOBILE") },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PhoneAndroid, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Switch to Mobile Remote Mode")
        }

        OutlinedButton(
            onClick = { viewModel.clearDeviceMode() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().testTag("tv_reset_button")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset Device Mode (Show First Start Setup)")
        }
    }
}

@Composable
fun NetflixDashboard(
    channels: List<ChannelEntity>,
    activeChannel: ChannelEntity?,
    viewModel: IptvViewModel,
    modifier: Modifier = Modifier
) {
    val featuredItem = remember(channels) {
        channels.firstOrNull { it.isFavorite } ?: channels.firstOrNull()
    }

    val favorites = remember(channels) { channels.filter { it.isFavorite } }
    val netflix = remember(channels) { channels.filter { it.name.contains("Netflix", ignoreCase = true) || it.groupTitle.contains("Netflix", ignoreCase = true) } }
    val disney = remember(channels) { channels.filter { it.name.contains("Disney", ignoreCase = true) || it.groupTitle.contains("Disney", ignoreCase = true) } }
    val otherChannels = remember(channels, netflix, disney) { channels.filter { it !in netflix && it !in disney } }
    val groups = remember(otherChannels) {
        otherChannels.groupBy { it.groupTitle }
            .filter { it.value.isNotEmpty() }
            .toList()
            .sortedByDescending { it.second.size }
            .take(6)
    }
    val topGroupChannels = remember(groups) { groups.flatMap { it.second }.toSet() }
    val remaining = remember(otherChannels, topGroupChannels) { otherChannels.filter { it !in topGroupChannels } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        if (featuredItem != null) {
            item {
                NetflixHeroBanner(featuredItem, activeChannel, viewModel)
            }
        }

        if (favorites.isNotEmpty()) {
            item {
                NetflixSwimlane(
                    title = "My List",
                    channels = favorites,
                    activeChannel = activeChannel,
                    viewModel = viewModel
                )
            }
        }

        if (netflix.isNotEmpty()) {
            item {
                NetflixSwimlane(
                    title = "Netflix Originals",
                    channels = netflix,
                    activeChannel = activeChannel,
                    viewModel = viewModel,
                    isOriginal = true
                )
            }
        }

        if (disney.isNotEmpty()) {
            item {
                NetflixSwimlane(
                    title = "Disney+ Magic",
                    channels = disney,
                    activeChannel = activeChannel,
                    viewModel = viewModel
                )
            }
        }

        items(groups) { group ->
            val groupName = group.first
            val groupList = group.second
            NetflixSwimlane(
                title = groupName,
                channels = groupList,
                activeChannel = activeChannel,
                viewModel = viewModel
            )
        }

        if (remaining.isNotEmpty()) {
            item {
                NetflixSwimlane(
                    title = "More For You",
                    channels = remaining,
                    activeChannel = activeChannel,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun NetflixHeroBanner(
    channel: ChannelEntity,
    activeChannel: ChannelEntity?,
    viewModel: IptvViewModel
) {
    val isPlaying = activeChannel?.id == channel.id

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF131722))
    ) {
        if (!channel.logoUrl.isNullOrEmpty()) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFE50914).copy(alpha = 0.3f), Color(0xFF0F1115))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(100.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color(0xFF0F1115)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            val streamType = remember(channel) { channel.getStreamType().name.replace("_", " ") }
            val qualityTag = remember(channel.name) {
                val upperName = channel.name.uppercase()
                when {
                    upperName.contains("4K") || upperName.contains("UHD") -> "4K"
                    upperName.contains("FHD") || upperName.contains("1080") -> "FHD"
                    upperName.contains("HD") || upperName.contains("720") -> "HD"
                    upperName.contains("SD") || upperName.contains("480") -> "SD"
                    else -> ""
                }
            }

            Row(
                modifier = Modifier.padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFE50914),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = channel.groupTitle.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = streamType,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                if (qualityTag.isNotEmpty()) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = qualityTag,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = channel.name,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.selectChannel(channel) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) Color(0xFFE50914) else Color.White,
                        contentColor = if (isPlaying) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "Playing" else "Play",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.toggleFavorite(channel) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.5.dp, Color.White),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = "My List"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "My List",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun NetflixSwimlane(
    title: String,
    channels: List<ChannelEntity>,
    activeChannel: ChannelEntity?,
    viewModel: IptvViewModel,
    isOriginal: Boolean = false
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(channels) { channel ->
                NetflixPosterCard(
                    channel = channel,
                    activeChannel = activeChannel,
                    viewModel = viewModel,
                    isOriginal = isOriginal
                )
            }
        }
    }
}

@Composable
fun NetflixPosterCard(
    channel: ChannelEntity,
    activeChannel: ChannelEntity?,
    viewModel: IptvViewModel,
    isOriginal: Boolean = false
) {
    var showInfoDialog by remember { mutableStateOf(false) }
    val isPlaying = activeChannel?.id == channel.id
    val scale by animateFloatAsState(targetValue = if (isPlaying) 1.05f else 1f, label = "poster_scale")
    
    var vodInfo by remember { mutableStateOf<org.json.JSONObject?>(null) }
    var isLoadingInfo by remember { mutableStateOf(false) }
    LaunchedEffect(showInfoDialog) {
        if (showInfoDialog && vodInfo == null) {
            isLoadingInfo = true
            vodInfo = viewModel.getVodOrSeriesInfo(channel.streamUrl)
            isLoadingInfo = false
        }
    }

    val deviceMode by viewModel.deviceMode.collectAsState()
    val isTvConnected by viewModel.isTvConnected.collectAsState()
    val cardWidth = if (isOriginal) 130.dp else 115.dp
    val cardHeight = if (isOriginal) 200.dp else 165.dp

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) Color(0xFFE50914).copy(alpha = 0.15f) else Color(0xFF1F232F)
        ),
        border = BorderStroke(
            width = if (isPlaying) 2.dp else 1.dp,
            color = if (isPlaying) Color(0xFFE50914) else Color.White.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .clickable { viewModel.selectChannel(channel) }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!channel.logoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF2C3246), Color(0xFF0F1115))
                            )
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            IconButton(
                onClick = { viewModel.toggleFavorite(channel) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(28.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (channel.isFavorite) Color.Red else Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = { showInfoDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            if (deviceMode == "MOBILE" && isTvConnected) {
                IconButton(
                    onClick = {
                        viewModel.sendRemoteCommand("PLAY_FEED|${channel.name}|${channel.streamUrl}|${channel.logoUrl ?: ""}|${channel.groupTitle}")
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cast,
                        contentDescription = "Cast to TV",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (isOriginal) {
                Surface(
                    color = Color(0xFFE50914),
                    shape = RoundedCornerShape(bottomEnd = 4.dp),
                    modifier = Modifier.align(Alignment.TopStart).offset(x = 28.dp)
                ) {
                    Text(
                        text = "N",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFFE50914).copy(alpha = 0.8f))
                            )
                        )
                        .align(Alignment.BottomCenter),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 8.sp
                        )
                    )
                }
            }

            val streamType = remember(channel) { channel.getStreamType().name.replace("_", " ") }
            val qualityTag = remember(channel.name) {
                val upperName = channel.name.uppercase()
                when {
                    upperName.contains("4K") || upperName.contains("UHD") -> "4K"
                    upperName.contains("FHD") || upperName.contains("1080") -> "FHD"
                    upperName.contains("HD") || upperName.contains("720") -> "HD"
                    upperName.contains("SD") || upperName.contains("480") -> "SD"
                    else -> ""
                }
            }

            val bottomPadding = if (isPlaying) 32.dp else 6.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .align(Alignment.BottomCenter)
                    .padding(start = 6.dp, end = 6.dp, bottom = bottomPadding + 20.dp, top = 12.dp)
            ) {
                Text(
                    text = channel.name,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 6.dp, bottom = bottomPadding),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = streamType,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                if (qualityTag.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFE50914).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = qualityTag,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(text = channel.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (!channel.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                        )
                    }
                    Text("Genre: ${channel.groupTitle}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    
                    if (isLoadingInfo) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (vodInfo != null) {
                        val info = vodInfo?.optJSONObject("info")
                        if (info != null) {
                            val description = info.optString("plot", "").takeIf { it.isNotBlank() } ?: info.optString("description", "No description available.")
                            val rating = info.optString("rating", "N/A")
                            val cast = info.optString("cast", "N/A")
                            val director = info.optString("director", "N/A")
                            val releaseDate = info.optString("release_date", "N/A")
                            
                            Text("Rating: $rating", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Release Date: $releaseDate", style = MaterialTheme.typography.bodySmall)
                            Text("Director: $director", style = MaterialTheme.typography.bodySmall)
                            Text("Cast: $cast", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Plot:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(description, style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Additional metadata is not available for this item.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text("Failed to load API info.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showInfoDialog = false
                        viewModel.selectChannel(channel)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) {
                    Text("Play Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
