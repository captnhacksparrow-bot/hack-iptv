import sys
import re

def main():
    with open("app/src/main/java/com/example/viewmodel/IptvViewModel.kt", "r") as f:
        content = f.read()

    # We want to remove the block I added which looks like:
    #                 // Auto-select the newly added playlist
    #                 val allPlaylists = repository.getStaticAllPlaylists()
    #                 val added = allPlaylists.find { it.url == url || it.url == "file://$url" || it.url == "asset://$url" }
    #                 if (added != null) {
    #                     _selectedPlaylistId.value = added.id
    #                 }
    # and the one for xtream
    
    # Actually, the original one works. I'll just remove the ones I added.
    
    pattern1 = r'\s*// Auto-select the newly added playlist\n\s*val allPlaylists = repository\.getStaticAllPlaylists\(\)\n\s*val added = allPlaylists\.find \{ it\.url == url \|\| it\.url == "file://\$url" \|\| it\.url == "asset://\$url" \}\n\s*if \(added != null\) \{\n\s*_selectedPlaylistId\.value = added\.id\n\s*\}\n'
    
    pattern2 = r'\s*// Auto-select the newly added playlist\n\s*val allPlaylists = repository\.getStaticAllPlaylists\(\)\n\s*val added = allPlaylists\.find \{ it\.url\.startsWith\("xtream://\$username"\) \}\n\s*if \(added != null\) \{\n\s*_selectedPlaylistId\.value = added\.id\n\s*\}\n'

    content = re.sub(pattern1, '\n', content)
    content = re.sub(pattern2, '\n', content)

    with open("app/src/main/java/com/example/viewmodel/IptvViewModel.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
