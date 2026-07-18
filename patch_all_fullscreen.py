import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.read()

    # 1. Update else if condition and block
    old_else_if = """    } else if (activeTab in listOf(7, 8, 10)) {
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
    } else {"""
    
    new_else_if = """    } else if (activeTab in listOf(4, 5, 6, 7, 8, 9, 10)) {
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
                    text = when(activeTab) { 
                        4 -> "Favorites"
                        5 -> "TV Guide"
                        6 -> "Recordings"
                        7 -> "Playlists"
                        8 -> "Account"
                        9 -> "Remote Control"
                        else -> "Settings" 
                    },
                    color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxSize()) {
                when(activeTab) {
                    4 -> FavoritesTab(viewModel)
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
    } else {"""
    content = content.replace(old_else_if, new_else_if)
    
    # 2. Remove 4, 5, 6, 7, 8, 9, 10 from the main content when block
    old_main_when = """                4 -> FavoritesTab(viewModel)
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
                10 -> {
                    // Settings tab if exists, else just empty for now
                }"""
                
    new_main_when = """                // Handled in full screen layer"""
    content = content.replace(old_main_when, new_main_when)

    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
