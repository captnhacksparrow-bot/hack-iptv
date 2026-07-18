import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.read()
        
    old_root = """    val skyBlueGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF00194A), Color(0xFF003D99))
    )

    if (isFullScreen) {"""
    
    new_root = """    val skyBlueGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF00194A), Color(0xFF003D99))
    )
    val deviceMode by viewModel.deviceMode.collectAsState()

    if (deviceMode == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(skyBlueGradient).safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                com.example.ui.components.CaptnHackLogo(modifier = Modifier.height(100.dp), showText = true, animate = true)
                Text("Select your device type", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    androidx.compose.material3.Button(onClick = { viewModel.setDeviceMode("TV") }) {
                        Icon(Icons.Default.Tv, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("TV Interface")
                    }
                    androidx.compose.material3.Button(onClick = { viewModel.setDeviceMode("MOBILE") }) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Mobile Interface")
                    }
                }
            }
        }
        return
    }

    if (isFullScreen) {"""
    
    content = content.replace(old_root, new_root)
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
