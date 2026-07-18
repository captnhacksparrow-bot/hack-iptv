import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        lines = f.readlines()

    start_idx = -1
    end_idx = -1
    for i, line in enumerate(lines):
        if "LazyRow(" in line and "Categories (Tabs)" in lines[i-1]:
            start_idx = i - 1
        if start_idx != -1 and "Spacer(modifier = Modifier.height(16.dp))" in line:
            end_idx = i
            break

    if start_idx != -1 and end_idx != -1:
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
        }
        """
        lines = lines[:start_idx] + [new_lazyrow] + lines[end_idx:]
        with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
            f.writelines(lines)
        print("Patched successfully")
    else:
        print("Could not find bounds")

if __name__ == "__main__":
    main()
