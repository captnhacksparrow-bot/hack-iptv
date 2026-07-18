import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        lines = f.readlines()

    # Find the line that says "                when(activeTab) {"
    # We'll just search for it manually in the first 500 lines.
    start_idx = -1
    for i in range(350, 450):
        if "when(activeTab) {" in lines[i] and "}" not in lines[i]:
            start_idx = i
            break
            
    print(f"Found when block at line {start_idx+1}")
    
    # We want to insert the cases here.
    cases = """                    4 -> FavoritesTab(viewModel)
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
"""

    lines.insert(start_idx + 1, cases)
    
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
        f.writelines(lines)

if __name__ == "__main__":
    main()
