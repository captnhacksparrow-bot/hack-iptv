import sys
import re

def main():
    with open("app/src/main/java/com/example/viewmodel/IptvViewModel.kt", "r") as f:
        content = f.read()

    new_add = """            val success = repository.addXtreamPlaylist(name, serverUrl, username, password)
            _isAddingPlaylist.value = false
            if (success) {
                // Auto-select the newly added playlist
                val allPlaylists = repository.playlists.value
                val added = allPlaylists.find { it.url.startsWith("xtream://$username") }
                if (added != null) {
                    _selectedPlaylistId.value = added.id
                }
"""
    content = re.sub(r'val success = repository\.addXtreamPlaylist\(name, serverUrl, username, password\)\n\s*_isAddingPlaylist\.value = false\n\s*if \(success\) \{', new_add, content)

    with open("app/src/main/java/com/example/viewmodel/IptvViewModel.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
