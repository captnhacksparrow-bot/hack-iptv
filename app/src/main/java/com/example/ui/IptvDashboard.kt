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
    
    // Live TV Overlay state
    var isOverlayVisible by remember { mutableStateOf(true) }
    
    // Auto-dismiss overlay after 5 seconds of inactivity
    LaunchedEffect(isOverlayVisible) {
        if (isOverlayVisible) {
            kotlinx.coroutines.delay(5000)
            isOverlayVisible = false
        }
    }


    var isFullScreenMobile by remember { mutableStateOf(false) }
    var showMobileOverlayMenu by remember { mutableStateOf(false) }
    var showMobileControls by remember { mutableStateOf(true) }

    LaunchedEffect(showMobileControls, showMobileOverlayMenu) {
        if (showMobileControls && !showMobileOverlayMenu) {
            kotlinx.coroutines.delay(3500)
            showMobileControls = false
        }
    }

    LaunchedEffect(activePlayUrl) {
        if (activePlayUrl != null) {
            isFullScreenMobile = autoFullscreen
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
                    modifier = Modifier.padding(32.dp),
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
            .background(Color(0xFF0F1115))
    ) {
        // Main content area - single layout
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 2. Main content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                TabContent(
                    deviceMode = deviceMode ?: "MOBILE",
                    activeTab = activeTab,
                    viewModel = viewModel
                )
                
                // Show overlay only if in Live TV tab (tab 0)
                if (activeTab == 0) {
                    LiveTvOverlay(visible = isOverlayVisible)
                }
            }
            
            // 1. Bottom Navigation Bar
            NavigationBar(
                containerColor = Color(0xFF131722),
                contentColor = Color.White
            ) {
                val deviceMode by viewModel.deviceMode.collectAsState()
                
                val navItems = mutableListOf(
                    "Live TV" to Icons.Default.Tv,
                    "PPV" to Icons.Default.Event,
                    "Movies" to Icons.Default.Movie,
                    "Shows" to Icons.Default.VideoLibrary,
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
                                0 -> viewModel.selectStreamType(StreamType.LIVE_TV)
                                1 -> viewModel.selectStreamType(StreamType.PPV)
                                2 -> viewModel.selectStreamType(StreamType.MOVIE)
                                3 -> viewModel.selectStreamType(StreamType.TV_SHOW)
                                // Handle other tabs
                            }
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        icon = { Icon(icon, contentDescription = label) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerSection(
    activePlayUrl: String?,
    selectedChannel: ChannelEntity?,
    isPlayingCatchup: Boolean,
    selectedProgram: EpgProgramEntity?,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (activePlayUrl != null && selectedChannel != null) {
        val streamType = selectedChannel.getStreamType()
        val subtitle = when {
            isPlayingCatchup && selectedProgram != null -> "Catchup: ${selectedProgram.title}"
            streamType == StreamType.MOVIE -> "Movie (On Demand)"
            streamType == StreamType.TV_SHOW -> "TV Show (On Demand)"
            streamType == StreamType.PPV -> "PPV Live Event"
            else -> "Live TV Broadcast"
        }
        val isLiveStream = (streamType == StreamType.LIVE_TV || streamType == StreamType.PPV) && !isPlayingCatchup

        VideoPlayer(
            videoUrl = activePlayUrl,
            title = selectedChannel.name,
            subtitle = subtitle,
            onDownloadClick = onDownloadClick,
            isLiveStream = isLiveStream,
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .testTag("video_player")
        )
    } else {
        // Placeholder state when no channels are loaded or selected
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF121824),
                            Color(0xFF07090C)
                        )
                    )
                )
                .border(1.dp, Color(0xFFFFB03A).copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            CaptnHackLogo(
                modifier = Modifier.padding(24.dp),
                showText = true,
                animate = true
            )
        }
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
                    modifier = Modifier.size(56.dp)
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
    viewModel: IptvViewModel
) {
    when (activeTab) {
        0 -> ChannelsExplorerTab(viewModel = viewModel)
        1 -> ChannelsExplorerTab(viewModel = viewModel)
        2 -> ChannelsExplorerTab(viewModel = viewModel)
        3 -> ChannelsExplorerTab(viewModel = viewModel)
        4 -> EpgGuideTab(viewModel = viewModel)
        5 -> RecordingsTab(viewModel = viewModel)
        6 -> PlaylistsTab(viewModel = viewModel)
        7 -> ProfileTab(viewModel = viewModel)
        8 -> {
            if (deviceMode == "MOBILE") {
                TvControllerTab(viewModel = viewModel)
            } else {
                TvConnectionsTab(viewModel = viewModel)
            }
        }
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

    var showCategorySidebarMobile by remember { mutableStateOf(false) }

    // Auto-dismiss sidebar after a few seconds of inactivity
    LaunchedEffect(showCategorySidebarMobile) {
        if (showCategorySidebarMobile) {
            kotlinx.coroutines.delay(5000)
            showCategorySidebarMobile = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar with Categories drawer trigger icon
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search channel name...") },
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
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("channel_search_bar")
            )

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
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("channel_list")
                ) {
                    items(channels) { channel ->
                        val isPlaying = activeChannel?.id == channel.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPlaying) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectChannel(channel) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChannelLogo(
                                    logoUrl = channel.logoUrl,
                                    channelName = channel.name,
                                    modifier = Modifier.size(48.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
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
                                        color = if (isPlaying) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }

                                if (deviceMode == "MOBILE" && isTvConnected) {
                                    IconButton(
                                        onClick = {
                                            viewModel.sendRemoteCommand("PLAY_FEED|${channel.name}|${channel.streamUrl}|${channel.logoUrl ?: ""}|${channel.groupTitle}")
                                        },
                                        modifier = Modifier.testTag("cast_channel_${channel.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cast,
                                            contentDescription = "Cast to TV",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                IconButton(onClick = { viewModel.toggleFavorite(channel) }) {
                                    Icon(
                                        imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite Toggle",
                                        tint = if (channel.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
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
            CategorySidebar(
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

                        // Status Badge
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

    val sampleUrl = "http://tvload.win/get.php?username=694788876178&password=023656073374&output=ts&type=m3u_plus"

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Playlist & Server Manager",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        Text(
            text = "Offline-First: All playlists, preferences, and favorites are stored locally in your device's private SQLite database. Internet is only required to load raw stream channels and remote commands.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

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
                label = { Text("Standard M3U URL / File") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            FilterChip(
                selected = isXtreamMode,
                onClick = { isXtreamMode = true },
                label = { Text("Xtream Codes API") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        if (!isXtreamMode) {
            // M3U Input Form
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                label = { Text("Playlist Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playlist_name_field")
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = playlistUrl,
                onValueChange = { playlistUrl = it },
                label = { Text("M3U Playlist URL or File URI") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Pick Local File",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playlist_url_field")
            )
        } else {
            // Xtream Codes Input Form
            OutlinedTextField(
                value = xtreamName,
                onValueChange = { xtreamName = it },
                label = { Text("Server Name (e.g. Premium Live)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = xtreamServer,
                onValueChange = { xtreamServer = it },
                label = { Text("Xtream Server URL (e.g. http://my-iptv.com:8080)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                    }
                } else {
                    if (xtreamName.isNotEmpty() && xtreamServer.isNotEmpty() && xtreamUser.isNotEmpty() && xtreamPass.isNotEmpty()) {
                        viewModel.addXtreamPlaylist(xtreamName, xtreamServer, xtreamUser, xtreamPass)
                        xtreamName = ""
                        xtreamServer = ""
                        xtreamUser = ""
                        xtreamPass = ""
                    }
                }
            },
            enabled = !isAdding && (
                (!isXtreamMode && playlistName.isNotEmpty() && playlistUrl.isNotEmpty()) ||
                (isXtreamMode && xtreamName.isNotEmpty() && xtreamServer.isNotEmpty() && xtreamUser.isNotEmpty() && xtreamPass.isNotEmpty())
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_playlist_button")
        ) {
            if (isAdding) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Syncing IPTV Metadata on Local DB...")
            } else {
                Text(if (isXtreamMode) "Connect & Sync Xtream API" else "Add M3U Playlist")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Active Playlists",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No playlists added yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("playlists_list")
            ) {
                items(playlists) { playlist ->
                    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsState()
                    val isActive = selectedPlaylistId == playlist.id
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        border = if (isActive) {
                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectPlaylist(playlist.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("Active") }
                                        )
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
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh Playlist",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Playlist",
                                        tint = MaterialTheme.colorScheme.error
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

@Composable
fun ProfileTab(viewModel: IptvViewModel) {
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

    var editUsername by remember { mutableStateOf(username) }
    var editPassword by remember { mutableStateOf(password) }
    var editServer by remember { mutableStateOf(serverUrl) }
    var editStatus by remember { mutableStateOf(if (status.isEmpty()) "Active" else status) }
    var editExpiry by remember { mutableStateOf(if (expiry.isEmpty()) "Lifetime" else expiry) }
    var editMaxConn by remember { mutableStateOf(maxConn) }
    var editActiveConn by remember { mutableStateOf(activeConn) }

    var showAddSubscription by remember { mutableStateOf(username.isNotEmpty()) }
    var isEditing by remember { mutableStateOf(false) }

    var localAvatarInput by remember { mutableStateOf(avatarUrl) }

    val context = LocalContext.current

    LaunchedEffect(username, password, serverUrl, status, expiry, maxConn, activeConn) {
        if (!isEditing) {
            editUsername = username
            editPassword = password
            editServer = serverUrl
            editStatus = if (status.isEmpty()) "Active" else status
            editExpiry = if (expiry.isEmpty()) "Lifetime" else expiry
            editMaxConn = maxConn
            editActiveConn = activeConn
        }
    }

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

        // YOU'RE A GUEST - EXPANDABLE IPTV SUBSCRIPTION BANNER
        if (username.isEmpty()) {
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
                            text = "YOU'RE A GUEST / Add paid subscription",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Right now you only have the free public iptv-org channels. Add your own Xtream code or M3U URL from any paid provider to unlock thousands of HD/4K channels, movies and series.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showAddSubscription = !showAddSubscription },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add Subscription", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // XTREAM CODES CREDENTIALS LOGIN CARD
        if (showAddSubscription || username.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (username.isEmpty()) "Connect Xtream IPTV Subscription" else "Subscription Credentials",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { editUsername = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPassword,
                        onValueChange = { editPassword = it },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editServer,
                        onValueChange = { editServer = it },
                        label = { Text("IPTV Portal/Server URL") },
                        placeholder = { Text("e.g. http://provider.com:8080") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editStatus,
                            onValueChange = { editStatus = it },
                            label = { Text("Status") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = editExpiry,
                            onValueChange = { editExpiry = it },
                            label = { Text("Expiry Date") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editActiveConn,
                            onValueChange = { editActiveConn = it },
                            label = { Text("Active Connections") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = editMaxConn,
                            onValueChange = { editMaxConn = it },
                            label = { Text("Max Connections") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (editUsername.isNotEmpty()) {
                                    viewModel.updatePremiumProfile(
                                        username = editUsername,
                                        password = editPassword,
                                        serverUrl = editServer,
                                        status = editStatus,
                                        expiry = editExpiry,
                                        maxConn = editMaxConn,
                                        activeConn = editActiveConn,
                                        type = "Custom Xtream Account",
                                        isMock = true
                                    )
                                    isEditing = false
                                    android.widget.Toast.makeText(context, "Credentials saved locally!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = editUsername.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Locally")
                        }

                        if (username.isNotEmpty()) {
                            Button(
                                onClick = {
                                    viewModel.clearPremiumProfile()
                                    editUsername = ""
                                    editPassword = ""
                                    editServer = ""
                                    editStatus = "Active"
                                    editExpiry = "Lifetime"
                                    editActiveConn = "0"
                                    editMaxConn = "1"
                                    android.widget.Toast.makeText(context, "Premium profile cleared.", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Profile")
                            }
                        }
                    }
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
    modifier: Modifier = Modifier
) {
    if (!logoUrl.isNullOrEmpty()) {
        AsyncImage(
            model = logoUrl,
            contentDescription = "$channelName logo",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.1f))
        )
    } else {
        // High-end initials fallback logo
        val initials = channelName.trim()
            .split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")

        Box(
            modifier = modifier
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
