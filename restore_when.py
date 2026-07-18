import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.read()

    chunk = """ 4 -> FavoritesTab(viewModel)
                    5 -> EpgGuideTab(viewModel)
                    6 -> RecordingsTab(viewModel)
                    7 -> PlaylistsTab(viewModel)
                    8 -> ProfileTab(viewModel, onNavigateToPlaylists = { activeTab = 7 })
                    9 -> {
                        val deviceMode by viewModel.deviceMode.collectAsState()
                        if (deviceMode == "TV") {
                            TvConnectionsTab(viewModel)
                        } else {
                            TvControllerTab(viewModel)
                        }
                    }
                    10 -> { /* SettingsTab placeholder */ }
                }
            }
        }
    } else {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(skyBlueGradient)
            .padding(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.example.ui.components.CaptnHackLogo(
                        modifier = Modifier.height(48.dp),
                        showText = true,
                        animate = false
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Home",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.width(32.dp))
                    // Add Playlist
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { activeTab = 7 }.padding(end = 16.dp)
                    ) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = "Playlists", tint = Color.White, modifier = Modifier.size(24.dp).padding(end=4.dp))
                        Text("Playlists", color = Color.White, fontSize = 16.sp)
                    }
                    // Account Details
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { activeTab = 8 }.padding(end = 16.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White, modifier = Modifier.size(24.dp).padding(end=4.dp))
                        Text("Account", color = Color.White, fontSize = 16.sp)
                    }
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(24.dp).clickable { activeTab = 10 })
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = selectedChannel?.name ?: "Welcome to Sky",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "Press SELECT to view",
                    color = Color.White.copy(alpha=0.7f),
                    fontSize = 16.sp
                )
            }
            // Mini TV Player
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .height(180.dp)
                    .background(Color.Black)
            ) {
                if (activePlayUrl != null) {
                    VideoPlayer(
                        videoUrl = activePlayUrl!!,
                        title = selectedChannel?.name ?: "Unknown Channel",
                        thumbnailUrl = null,
                        subtitle = null,
                        onDownloadClick = {},
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "No Signal",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                // Invisible overlay to catch clicks for full screen
                Box(modifier = Modifier.fillMaxSize().clickable { isFullScreen = true })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "On Demand",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // Categories (Tabs)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF002266)),
            horizontalArrangement = Arrangement.Start
        ) {
            items(tvNavItems.size) { index ->
                val label = tvNavItems[index]
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .background(if (isSelected) Color(0xFF003D99) else Color.Transparent)
                        .clickable { 
                            activeTab = index 
                            when (index) {
                                0 -> viewModel.selectStreamType(StreamType.LIVE_TV)
                                1 -> viewModel.selectStreamType(StreamType.PPV)
                                2 -> viewModel.selectStreamType(StreamType.TV_SHOW)
                                3 -> viewModel.selectStreamType(StreamType.MOVIE)
                            }
                        }
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
"""
    
    # We replaced it with "            }\n        }\n\n"
    # Actually wait. In the file right now, it says:
    # "            }\n        }\n\n        Spacer(modifier = Modifier.height(16.dp))"
    
    content = content.replace("            }\n        }\n\n", chunk)

    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
