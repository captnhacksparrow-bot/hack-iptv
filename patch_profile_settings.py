import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.read()

    # Collect states in ProfileTab
    old_states = """    val autoplayNext by viewModel.autoplayNext.collectAsState()
    val autoFullscreen by viewModel.autoFullscreen.collectAsState()
    val showTmdb by viewModel.showTmdb.collectAsState()"""
    
    new_states = """    val autoplayNext by viewModel.autoplayNext.collectAsState()
    val autoFullscreen by viewModel.autoFullscreen.collectAsState()
    val showTmdb by viewModel.showTmdb.collectAsState()
    val dolbyAudio by viewModel.dolbyAudio.collectAsState()
    val enableCc by viewModel.enableCc.collectAsState()
    val useVlcPlayer by viewModel.useVlcPlayer.collectAsState()"""

    content = content.replace(old_states, new_states)

    # Add the rows to Playback Preferences
    old_switches = """                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Rich metadata (TMDB)", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = showTmdb,
                                    onCheckedChange = { viewModel.setShowTmdb(it) }
                                )
                            }
                        }
                    }
                }
            }"""

    new_switches = """                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Rich metadata (TMDB)", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = showTmdb,
                                    onCheckedChange = { viewModel.setShowTmdb(it) }
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Use VLC Player (External)", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = useVlcPlayer,
                                    onCheckedChange = { viewModel.setUseVlcPlayer(it) }
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Dolby Audio Passthrough", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = dolbyAudio,
                                    onCheckedChange = { viewModel.setDolbyAudio(it) }
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enable Closed Captions (CC)", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = enableCc,
                                    onCheckedChange = { viewModel.setEnableCc(it) }
                                )
                            }
                        }
                    }
                }
            }"""
    content = content.replace(old_switches, new_switches)

    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
