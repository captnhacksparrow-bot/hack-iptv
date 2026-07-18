import sys

def main():
    file_path = "app/src/main/java/com/example/ui/IptvDashboard.kt"
    with open(file_path, "r") as f:
        content = f.read()

    # We know the extra brace is at the end:
    #         }
    #     }
    #     } // End of isFullScreen else block
    # }
    
    # Let's find this and fix it.
    old_end_block = """        }
    }
    } // End of isFullScreen else block
}

@Composable
fun ScreenCastDialog("""
    new_end_block = """        }
    }
}

@Composable
fun ScreenCastDialog("""
    
    if old_end_block in content:
        content = content.replace(old_end_block, new_end_block)
    
    # Now let's apply the if (isFullScreen) around the Column
    # It starts at:
    #     val skyBlueGradient = Brush.verticalGradient(
    #         colors = listOf(Color(0xFF00194A), Color(0xFF003D99))
    #     )
    # 
    #     Column(

    old_root_start = """    val skyBlueGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF00194A), Color(0xFF003D99))
    )

    Column("""
    
    new_root_start = """    val skyBlueGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF00194A), Color(0xFF003D99))
    )

    if (isFullScreen) {
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
    
    if old_root_start in content:
        content = content.replace(old_root_start, new_root_start)
        # Add the closing brace for else block
        # We replace the ending again
        old_end_block_2 = """        }
    }
}

@Composable
fun ScreenCastDialog("""
        new_end_block_2 = """        }
    }
    } // End of isFullScreen else block
}

@Composable
fun ScreenCastDialog("""
        content = content.replace(old_end_block_2, new_end_block_2)

    with open(file_path, "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
