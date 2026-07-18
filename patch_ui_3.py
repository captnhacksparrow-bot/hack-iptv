import sys

def main():
    file_path = "app/src/main/java/com/example/ui/IptvDashboard.kt"
    with open(file_path, "r") as f:
        content = f.read()

    # 1. Add isFullScreen state
    old_state = "var activeTab by remember { mutableIntStateOf(0) }"
    new_state = "var activeTab by remember { mutableIntStateOf(0) }\n    var isFullScreen by remember { mutableStateOf(false) }"
    content = content.replace(old_state, new_state)

    # 2. Make Mini TV player clickable
    old_mini_player = """            // Mini TV Player
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .height(180.dp)
                    .background(Color.Black)
            ) {"""
    new_mini_player = """            // Mini TV Player
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .height(180.dp)
                    .background(Color.Black)
                    .clickable { isFullScreen = true }
            ) {"""
    content = content.replace(old_mini_player, new_mini_player)
    
    # 3. Add when(activeTab) for Main Content Area
    old_main_content = """        // Main Content Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Categories Sidebar (Sky Style for internal categories)"""
                
    new_main_content = """        // Main Content Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeTab) {
                0, 1, 2, 3 -> {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Categories Sidebar (Sky Style for internal categories)"""
    content = content.replace(old_main_content, new_main_content)
    
    old_main_content_end = """                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Nav (Colored Buttons)"""
        
    new_main_content_end = """                        }
                    }
                }
            }
                }
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
                10 -> {
                    // Settings tab if exists, else just empty for now
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Nav (Colored Buttons)"""
    content = content.replace(old_main_content_end, new_main_content_end)

    # 4. Wrap everything in a full screen check
    old_root_column = """    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF003D99)) // Sky Blue base background
            .padding(16.dp)
    ) {"""
    new_root_column = """    if (isFullScreen) {
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF003D99)) // Sky Blue base background
            .padding(16.dp)
    ) {"""
    content = content.replace(old_root_column, new_root_column)
    
    # Close the else block at the end of IptvDashboard
    old_end = """        }
    }
}

@Composable
fun ScreenCastDialog("""
    new_end = """        }
    }
    } // End of isFullScreen else block
}

@Composable
fun ScreenCastDialog("""
    content = content.replace(old_end, new_end)

    with open(file_path, "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
