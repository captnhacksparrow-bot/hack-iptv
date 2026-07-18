import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.read()

    # Need to add favorites icon to the channel card
    old_card_content = """                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                                    if (isSelected) {
                                        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFC107)).padding(vertical = 4.dp)) {
                                            Text(
                                                text = channel.name, 
                                                color = Color.Black, 
                                                fontWeight = FontWeight.Bold, 
                                                modifier = Modifier.align(Alignment.Center), 
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = channel.name,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }"""
    
    new_card_content = """                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                                        if (isSelected) {
                                            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFC107)).padding(vertical = 4.dp)) {
                                                Text(
                                                    text = channel.name, 
                                                    color = Color.Black, 
                                                    fontWeight = FontWeight.Bold, 
                                                    modifier = Modifier.align(Alignment.Center), 
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = channel.name,
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                    // Favorites toggle
                                    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
                                    val isFavorite = favoriteChannels.any { it.url == channel.url }
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(channel) },
                                        modifier = Modifier.align(Alignment.BottomEnd).size(32.dp).padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Toggle Favorite",
                                            tint = if (isFavorite) Color.Red else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }"""
    
    content = content.replace(old_card_content, new_card_content)

    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
