import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.read()

    old_epg = """fun EpgGuideTab(viewModel: IptvViewModel) {
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val epgPrograms by viewModel.epgPrograms.collectAsState()"""
    
    # We want to change EpgGuideTab to show all channels if no channel is selected,
    # or just show a grid. Actually, `filteredChannels` is available.
    
    new_epg = """fun EpgGuideTab(viewModel: IptvViewModel) {
    val channels by viewModel.filteredChannels.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val now = remember { System.currentTimeMillis() }
    val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "TV Guide",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (channels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No channels available.", color = Color.Gray)
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(channels) { channel ->
                    // Instead of Flow, we just observe if they select it, or we could just show current program
                    // Since EPG might be massive, let's keep it simple.
                    val isSelected = selectedChannel?.id == channel.id
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { viewModel.selectChannel(channel) },
                        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF003D99) else Color(0xFF1E1E1E))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(channel.name, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.3f))
                            Spacer(modifier = Modifier.width(12.dp))
                            if (isSelected) {
                                val epgPrograms by viewModel.epgPrograms.collectAsState()
                                androidx.compose.foundation.lazy.LazyRow(modifier = Modifier.weight(0.7f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(epgPrograms) { program ->
                                        val isPast = program.endTime <= now
                                        val isCurrent = now in program.startTime..program.endTime
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isCurrent) Color(0xFFE5B914).copy(alpha = 0.2f) else Color.Transparent,
                                                    shape = MaterialTheme.shapes.small
                                                )
                                                .clickable(enabled = isPast || isCurrent) {
                                                    if (isPast) {
                                                        viewModel.playCatchupProgram(program)
                                                    } else if (isCurrent) {
                                                        viewModel.selectChannel(channel)
                                                    }
                                                }
                                                .padding(8.dp)
                                                .widthIn(min = 120.dp, max = 200.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    "${timeFormat.format(java.util.Date(program.startTime))} - ${timeFormat.format(java.util.Date(program.endTime))}",
                                                    color = if (isCurrent) Color(0xFFE5B914) else Color.Gray,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    program.title,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isPast && channel.catchupDays > 0) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.History, contentDescription = "Catchup", tint = Color.Green, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text("Catch Up", color = Color.Green, fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("Select to view programs", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}
"""
    
    # We need to replace the entire EpgGuideTab
    start_str = "fun EpgGuideTab(viewModel: IptvViewModel) {"
    end_str = "fun RecordingsTab(viewModel: IptvViewModel) {"
    
    start_idx = content.find(start_str)
    end_idx = content.find(end_str)
    
    if start_idx != -1 and end_idx != -1:
        # Check for any @Composable before RecordingsTab
        comp_idx = content.rfind("@Composable", start_idx, end_idx)
        if comp_idx != -1:
            end_idx = comp_idx
        content = content[:start_idx] + new_epg + content[end_idx:]
        
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
