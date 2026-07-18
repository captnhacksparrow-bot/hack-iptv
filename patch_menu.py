import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.read()

    # 1. Update tvNavItems
    old_nav = """    val tvNavItems = remember { listOf(
        "Live",
        "PPV",
        "Series",
        "Movies"
    ) }"""
    new_nav = """    val tvNavItems = remember { listOf(
        Pair("Live", 0),
        Pair("PPV", 1),
        Pair("Series", 2),
        Pair("Movies", 3),
        Pair("Playlists", 7)
    ) }"""
    content = content.replace(old_nav, new_nav)

    # 2. Update LazyRow items
    old_lazyrow = """        // Categories (Tabs)
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
        }"""
    new_lazyrow = """        // Categories (Tabs)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF002266)),
            horizontalArrangement = Arrangement.Start
        ) {
            items(tvNavItems.size) { index ->
                val (label, tabIndex) = tvNavItems[index]
                val isSelected = activeTab == tabIndex
                Box(
                    modifier = Modifier
                        .background(if (isSelected) Color(0xFF003D99) else Color.Transparent)
                        .clickable { 
                            activeTab = tabIndex
                            when (tabIndex) {
                                0 -> viewModel.selectStreamType(StreamType.LIVE_TV)
                                1 -> viewModel.selectStreamType(StreamType.PPV)
                                2 -> viewModel.selectStreamType(StreamType.TV_SHOW)
                                3 -> viewModel.selectStreamType(StreamType.MOVIE)
                            }
                        }
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }"""
    content = content.replace(old_lazyrow, new_lazyrow)

    # 3. Sidebar text size
    old_sidebar = """                            Text(
                                text = category,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )"""
    new_sidebar = """                            Text(
                                text = category,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )"""
    content = content.replace(old_sidebar, new_sidebar)

    # 4. Make sidebar wider (was 180.dp, make it 240.dp)
    old_grid = """                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier.width(180.dp).fillMaxHeight(),"""
    new_grid = """                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier.width(260.dp).fillMaxHeight(),"""
    content = content.replace(old_grid, new_grid)

    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
