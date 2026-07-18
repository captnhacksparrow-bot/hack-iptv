import sys

def main():
    with open("app/src/main/java/com/example/data/IptvRepository.kt", "r") as f:
        content = f.read()
        
    old_update = """                val channels = M3uParser.parse(bodyString, id)
                if (channels.isNotEmpty()) {
                    iptvDao.insertChannels(channels)
                    return@withContext true
                }"""
    
    new_update = """                val channels = M3uParser.parse(bodyString, id)
                if (channels.isNotEmpty()) {
                    val oldFavorites = iptvDao.getFavoriteChannelsStatic(id).map { it.streamUrl }.toSet()
                    val updatedChannels = channels.map { if (it.streamUrl in oldFavorites) it.copy(isFavorite = true) else it }
                    iptvDao.insertChannels(updatedChannels)
                    return@withContext true
                }"""
    
    content = content.replace(old_update, new_update)
    
    old_xtream = """            if (channels.isNotEmpty()) {
                iptvDao.insertChannels(channels)
            }"""
    
    new_xtream = """            if (channels.isNotEmpty()) {
                val oldFavorites = iptvDao.getFavoriteChannelsStatic(playlistId).map { it.streamUrl }.toSet()
                val updatedChannels = channels.map { if (it.streamUrl in oldFavorites) it.copy(isFavorite = true) else it }
                iptvDao.insertChannels(updatedChannels)
            }"""
            
    content = content.replace(old_xtream, new_xtream)
    
    with open("app/src/main/java/com/example/data/IptvRepository.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
