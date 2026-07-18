import sys, re

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.read()

    # Add import for BackHandler if not there
    if "import androidx.activity.compose.BackHandler" not in content:
        content = content.replace("import androidx.compose.ui.Alignment", "import androidx.activity.compose.BackHandler\nimport androidx.compose.ui.Alignment")

    # Replace the fullscreen block
    old_fullscreen = """    if (isFullScreen) {
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
        }"""
        
    new_fullscreen = """    if (isFullScreen) {
        BackHandler { isFullScreen = false }
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
        }"""

    content = content.replace(old_fullscreen, new_fullscreen)

    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
