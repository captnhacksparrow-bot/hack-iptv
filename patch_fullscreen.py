import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.read()

    # 1. Update full screen and tabs logic
    old_root_start = """    if (isFullScreen) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            if (activePlayUrl != null) {
                VideoPlayer(
                    videoUrl = activePlayUrl!!,
                    title = selectedChannel?.name ?: "Unknown Channel",
                    thumbnailUrl = null,
                    subtitle = null,
                    onDownloadClick = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Invisible box on top to capture click and restore UI
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isFullScreen = false }
            )
        }
    } else {
    Column("""
    
    new_root_start = """    if (isFullScreen) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            if (activePlayUrl != null) {
                VideoPlayer(
                    videoUrl = activePlayUrl!!,
                    title = selectedChannel?.name ?: "Unknown Channel",
                    thumbnailUrl = null,
                    subtitle = null,
                    onDownloadClick = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Invisible box on top to capture click and restore UI
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isFullScreen = false }
            )
        }
    } else if (activeTab in listOf(7, 8, 10)) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(skyBlueGradient)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { activeTab = 0 }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when(activeTab) { 7 -> "Playlists"; 8 -> "Account"; else -> "Settings" },
                    color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxSize()) {
                when(activeTab) {
                    7 -> PlaylistsTab(viewModel)
                    8 -> ProfileTab(viewModel, onNavigateToPlaylists = { activeTab = 7 })
                    10 -> { /* SettingsTab placeholder */ }
                }
            }
        }
    } else {
    Column("""
    content = content.replace(old_root_start, new_root_start)
    
    # 2. Fix the mini player clicking
    old_mini_player = """            // Mini TV Player
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .height(180.dp)
                    .background(Color.Black)
                    .clickable { isFullScreen = true }
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
            }"""
            
    new_mini_player = """            // Mini TV Player
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
            }"""
    content = content.replace(old_mini_player, new_mini_player)

    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
