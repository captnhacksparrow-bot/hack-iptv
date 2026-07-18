import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.read()

    old_empty = """                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No folders found", color = Color.Gray)
                    }"""
    
    new_empty = """                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                            Text("No playlists found.", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Please go to Playlists to add an M3U or Xtream codes playlist.", color = Color.Gray)
                        }
                    }"""

    content = content.replace(old_empty, new_empty)

    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
