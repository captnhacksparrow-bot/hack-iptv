import sys

def main():
    with open("app/src/main/java/com/example/data/IptvRepository.kt", "r") as f:
        content = f.read()
        
    old_fetch = """            // 4. Fetch VOD (Movies, fails gracefully)
            try {
                val vodStreamsUrl = "$serverUrl/player_api.php?username=$username&password=$password&action=get_vod_streams"
                val vodStreamsArray = fetchJsonArray(vodStreamsUrl)
                if (vodStreamsArray != null) {
                    for (i in 0 until vodStreamsArray.length()) {
                        try {
                            val obj = vodStreamsArray.getJSONObject(i)
                            val streamId = obj.optString("stream_id")
                            val streamName = obj.optString("name")
                            val categoryId = obj.optString("category_id")
                            val streamIcon = obj.optString("stream_icon")
                            val extension = obj.optString("container_extension", "mp4")
                            
                            val groupTitle = vodCatMap[categoryId] ?: "Movies"
                            val streamUrl = "$serverUrl/movie/$username/$password/$streamId.$extension"
                            val detectedCountry = M3uParser.detectCountry(streamName, groupTitle, null)

                            channels.add(
                                ChannelEntity(
                                    playlistId = playlistId,
                                    name = streamName,
                                    logoUrl = if (streamIcon.isNotEmpty()) streamIcon else null,
                                    groupTitle = groupTitle,
                                    streamUrl = streamUrl,
                                    tvgId = null,
                                    tvgName = null,
                                    catchupType = null,
                                    catchupSource = null,
                                    country = detectedCountry
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }"""

    new_fetch = """            // 4. Fetch VOD (Movies, fails gracefully)
            try {
                val vodStreamsUrl = "$serverUrl/player_api.php?username=$username&password=$password&action=get_vod_streams"
                val vodStreamsArray = fetchJsonArray(vodStreamsUrl)
                if (vodStreamsArray != null) {
                    for (i in 0 until vodStreamsArray.length()) {
                        try {
                            val obj = vodStreamsArray.getJSONObject(i)
                            val streamId = obj.optString("stream_id")
                            val streamName = obj.optString("name")
                            val categoryId = obj.optString("category_id")
                            val streamIcon = obj.optString("stream_icon")
                            val extension = obj.optString("container_extension", "mp4")
                            
                            val groupTitle = vodCatMap[categoryId] ?: "Movies"
                            val streamUrl = "$serverUrl/movie/$username/$password/$streamId.$extension"
                            val detectedCountry = M3uParser.detectCountry(streamName, groupTitle, null)

                            channels.add(
                                ChannelEntity(
                                    playlistId = playlistId,
                                    name = streamName,
                                    logoUrl = if (streamIcon.isNotEmpty()) streamIcon else null,
                                    groupTitle = groupTitle,
                                    streamUrl = streamUrl,
                                    tvgId = null,
                                    tvgName = null,
                                    catchupType = null,
                                    catchupSource = null,
                                    country = detectedCountry
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // 5. Fetch Series categories
            val seriesCatMap = mutableMapOf<String, String>()
            try {
                val seriesCatUrl = "$serverUrl/player_api.php?username=$username&password=$password&action=get_series_categories"
                val seriesCatsArray = fetchJsonArray(seriesCatUrl)
                if (seriesCatsArray != null) {
                    for (i in 0 until seriesCatsArray.length()) {
                        try {
                            val obj = seriesCatsArray.getJSONObject(i)
                            val catId = obj.optString("category_id")
                            val catName = obj.optString("category_name")
                            if (catId.isNotEmpty() && catName.isNotEmpty()) {
                                seriesCatMap[catId] = catName
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // 6. Fetch Series (fails gracefully)
            try {
                val seriesUrl = "$serverUrl/player_api.php?username=$username&password=$password&action=get_series"
                val seriesArray = fetchJsonArray(seriesUrl)
                if (seriesArray != null) {
                    for (i in 0 until seriesArray.length()) {
                        try {
                            val obj = seriesArray.getJSONObject(i)
                            val streamId = obj.optString("series_id")
                            val streamName = obj.optString("name")
                            val categoryId = obj.optString("category_id")
                            val streamIcon = obj.optString("cover")
                            
                            val groupTitle = seriesCatMap[categoryId] ?: "Series"
                            val streamUrl = "$serverUrl/series/$username/$password/$streamId"
                            val detectedCountry = M3uParser.detectCountry(streamName, groupTitle, null)

                            channels.add(
                                ChannelEntity(
                                    playlistId = playlistId,
                                    name = streamName,
                                    logoUrl = if (streamIcon.isNotEmpty()) streamIcon else null,
                                    groupTitle = groupTitle,
                                    streamUrl = streamUrl,
                                    tvgId = null,
                                    tvgName = null,
                                    catchupType = null,
                                    catchupSource = null,
                                    country = detectedCountry
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }"""

    content = content.replace(old_fetch, new_fetch)
    with open("app/src/main/java/com/example/data/IptvRepository.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
