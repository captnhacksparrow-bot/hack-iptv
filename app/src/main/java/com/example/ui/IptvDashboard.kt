package com.example.ui

import android.text.format.Formatter
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.ChannelEntity
import com.example.data.DownloadEntity
import com.example.data.EpgProgramEntity
import com.example.data.PlaylistEntity
import com.example.data.StreamType
import com.example.data.getStreamType
import com.example.ui.components.VideoPlayer
import com.example.ui.components.CaptnHackLogo
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

    var activeTab by remember { mutableIntStateOf(0) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth > 600.dp

        if (isWideScreen) {
            // Tablet / Desktop layout (Side-by-side)
            Row(modifier = Modifier.fillMaxSize()) {
                // Left 55%: Player + Title Card
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    PlayerSection(
                        activePlayUrl = activePlayUrl,
                        selectedChannel = selectedChannel,
                        isPlayingCatchup = isPlayingCatchup,
                        selectedProgram = selectedProgram,
                        onDownloadClick = { viewModel.downloadActiveStream() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ActiveChannelDetailsCard(
                        selectedChannel = selectedChannel,
                        isPlayingCatchup = isPlayingCatchup,
                        selectedProgram = selectedProgram,
                        onFavoriteToggle = { channel -> viewModel.toggleFavorite(channel) },
                        onDownloadToggle = { viewModel.downloadActiveStream() }
                    )
                }

                // Right 45%: Collateral interactive panel
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    DashboardTabsHeader(
                        activeTab = activeTab,
                        onTabSelected = { 
                            activeTab = it 
                            when (it) {
                                0 -> viewModel.selectStreamType(StreamType.LIVE_TV)
                                1 -> viewModel.selectStreamType(StreamType.PPV)
                                2 -> viewModel.selectStreamType(StreamType.MOVIE)
                                3 -> viewModel.selectStreamType(StreamType.TV_SHOW)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TabContent(
                        activeTab = activeTab,
                        viewModel = viewModel
                    )
                }
            }
        } else {
            // Mobile layout (Vertical)
            Column(modifier = Modifier.fillMaxSize()) {
                PlayerSection(
                    activePlayUrl = activePlayUrl,
                    selectedChannel = selectedChannel,
                    isPlayingCatchup = isPlayingCatchup,
                    selectedProgram = selectedProgram,
                    onDownloadClick = { viewModel.downloadActiveStream() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ActiveChannelDetailsCard(
                    selectedChannel = selectedChannel,
                    isPlayingCatchup = isPlayingCatchup,
                    selectedProgram = selectedProgram,
                    onFavoriteToggle = { channel -> viewModel.toggleFavorite(channel) },
                    onDownloadToggle = { viewModel.downloadActiveStream() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                DashboardTabsHeader(
                    activeTab = activeTab,
                    onTabSelected = { 
                        activeTab = it 
                        when (it) {
                            0 -> viewModel.selectStreamType(StreamType.LIVE_TV)
                            1 -> viewModel.selectStreamType(StreamType.PPV)
                            2 -> viewModel.selectStreamType(StreamType.MOVIE)
                            3 -> viewModel.selectStreamType(StreamType.TV_SHOW)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    TabContent(
                        activeTab = activeTab,
                        viewModel = viewModel
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
    onDownloadClick: () -> Unit
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
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .testTag("video_player")
        )
    } else {
        // Placeholder state when no channels are loaded or selected
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
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
    activeTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        "Live TV" to Icons.Default.Tv,
        "PPV / Events" to Icons.Default.Event,
        "Movies" to Icons.Default.Movie,
        "TV Shows" to Icons.Default.VideoLibrary,
        "EPG Guide" to Icons.Default.LiveTv,
        "Recordings" to Icons.Default.CloudDownload,
        "Playlists" to Icons.Default.PlaylistPlay
    )

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
    }
}

@Composable
fun ChannelsExplorerTab(viewModel: IptvViewModel) {
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val channels by viewModel.filteredChannels.collectAsState()
    val activeChannel by viewModel.selectedChannel.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search channel name...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
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

        // Horizontal Category slide
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
    val progresses by viewModel.downloadProgresses.collectAsState()
    val context = LocalContext.current

    if (downloads.isEmpty() && progresses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = "Recordings Empty",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No recordings yet.",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Press the Download button on the video player above to record/download the stream.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "My Recorded Streams",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Active progressing downloads
            items(progresses.toList()) { (url, progress) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recording stream...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(progress * 100).toInt()}% loaded (Safe Live Feed Sample)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Completed / Saved Downloads
            items(downloads) { download ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = download.channelName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Size: ${Formatter.formatFileSize(context, download.fileSize)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(download.downloadTime))
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (download.status == "COMPLETED") {
                                Button(
                                    onClick = {
                                        // Playing local recording using ExoPlayer
                                        // Selected channel plays the downloaded file
                                        viewModel.selectChannel(
                                            ChannelEntity(
                                                id = -999, // dummy ID
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
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play Recording", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play")
                                }
                            } else if (download.status == "FAILED") {
                                Badge(containerColor = Color.Red, modifier = Modifier.padding(end = 8.dp)) {
                                    Text("FAILED", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                                }
                            }

                            IconButton(onClick = { viewModel.deleteDownload(download) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Recording",
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

@Composable
fun PlaylistsTab(viewModel: IptvViewModel) {
    val playlists by viewModel.playlists.collectAsState()
    val isAdding by viewModel.isAddingPlaylist.collectAsState()
    val errorMsg by viewModel.playlistAddError.collectAsState()

    var playlistName by remember { mutableStateOf("") }
    var playlistUrl by remember { mutableStateOf("") }

    val sampleUrl = "http://tvload.win/get.php?username=694788876178&password=023656073374&output=ts&type=m3u_plus"

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "M3U Playlist Manager",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Input Form
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
            label = { Text("M3U Playlist URL") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("playlist_url_field")
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (errorMsg != null) {
            Text(
                text = errorMsg!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Button(
            onClick = {
                if (playlistName.isNotEmpty() && playlistUrl.isNotEmpty()) {
                    viewModel.addPlaylist(playlistName, playlistUrl)
                    playlistName = ""
                    playlistUrl = ""
                }
            },
            enabled = !isAdding && playlistName.isNotEmpty() && playlistUrl.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_playlist_button")
        ) {
            if (isAdding) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Downloading & Parsing Feed...")
            } else {
                Text("Add Playlist")
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
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
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
