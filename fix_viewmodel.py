import sys
import re

def main():
    with open("app/src/main/java/com/example/viewmodel/IptvViewModel.kt", "r") as f:
        content = f.read()

    content = content.replace("val allPlaylists = repository.playlists.value", "val allPlaylists = repository.getStaticAllPlaylists()")

    with open("app/src/main/java/com/example/viewmodel/IptvViewModel.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
