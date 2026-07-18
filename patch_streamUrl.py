import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.read()

    # Replace `.url` with `.streamUrl` for the favorites check
    old_check = "val isFavorite = favoriteChannels.any { it.url == channel.url }"
    new_check = "val isFavorite = favoriteChannels.any { it.streamUrl == channel.streamUrl }"
    
    content = content.replace(old_check, new_check)

    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
