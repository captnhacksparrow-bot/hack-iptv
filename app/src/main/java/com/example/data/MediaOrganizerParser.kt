package com.example.data

object MediaOrganizerParser {
    
    data class MediaMetadata(
        val cleanName: String,
        val resolution: String?, // "4K", "1080p", "720p", "SD", etc.
        val year: Int?, // Movie/Show year
        val season: Int?, // Season number
        val episode: Int?, // Episode number
        val language: String?, // Language tag (e.g. EN, FR, ES)
        val codec: String?, // "HEVC", "H265", etc.
        val isBackupChannel: Boolean
    )

    fun parseMetadata(rawName: String): MediaMetadata {
        var name = rawName.trim()
        
        // 1. Detect language / country tags in brackets or parenthesis
        val langRegex = Regex("""(?i)\[([A-Z]{2,3})\]|\(([A-Z]{2,3})\)|([A-Z]{3})\s*:|([A-Z]{2,3})\s*\|""")
        val langMatch = langRegex.find(name)
        val language = langMatch?.let {
            it.groups[1]?.value ?: it.groups[2]?.value ?: it.groups[3]?.value ?: it.groups[4]?.value
        }?.uppercase()

        if (langMatch != null) {
            name = name.replace(langMatch.value, "").trim()
        }

        // 2. Detect Backup/Alt channels
        val isBackup = name.lowercase().contains("backup") || 
                       name.lowercase().contains("alt") || 
                       name.lowercase().contains("temp") || 
                       name.contains("FHD-B")

        // 3. Detect resolution/quality tags
        val resolutionRegex = Regex("""(?i)\b(4k|uhd|1080p|720p|576p|480p|fhd|hd|sd)\b""")
        val resMatch = resolutionRegex.find(name)
        val resolution = resMatch?.value?.uppercase()

        if (resMatch != null) {
            name = name.replace(resMatch.value, "").trim()
        }

        // 4. Detect codecs
        val codecRegex = Regex("""(?i)\b(hevc|h265|h264|x265|x264|aac|ac3)\b""")
        val codecMatch = codecRegex.find(name)
        val codec = codecMatch?.value?.uppercase()
        if (codecMatch != null) {
            name = name.replace(codecMatch.value, "").trim()
        }

        // 5. Detect Year of release
        val yearRegex = Regex("""\b(19\d{2}|20\d{2})\b""")
        val yearMatch = yearRegex.find(name)
        val year = yearMatch?.value?.toIntOrNull()
        if (yearMatch != null) {
            name = name.replace(yearMatch.value, "").trim()
        }

        // 6. Detect Season and Episode patterns
        val s01e01Regex = Regex("""(?i)\bS(\d{1,2})\s*E(\d{1,3})\b|\bS(\d{1,2})\s*EP\s*(\d{1,3})\b|\bSeason\s*(\d{1,2})\s*Episode\s*(\d{1,3})\b""")
        val seMatch = s01e01Regex.find(name)
        var season: Int? = null
        var episode: Int? = null
        if (seMatch != null) {
            season = (seMatch.groups[1]?.value ?: seMatch.groups[3]?.value ?: seMatch.groups[5]?.value)?.toIntOrNull()
            episode = (seMatch.groups[2]?.value ?: seMatch.groups[4]?.value ?: seMatch.groups[6]?.value)?.toIntOrNull()
            name = name.replace(seMatch.value, "").trim()
        } else {
            // Check standalone episode/part
            val epRegex = Regex("""(?i)\b(?:EP|EPISODE|PART)\s*(\d{1,3})\b""")
            val epMatch = epRegex.find(name)
            if (epMatch != null) {
                episode = epMatch.groups[1]?.value?.toIntOrNull()
                name = name.replace(epMatch.value, "").trim()
            }
        }

        // Clean up symbols, brackets, double spaces from name
        var clean = name
            .replace(Regex("""[\[\({]-?[\s]*[-?\]\)}]"""), "") // Empty brackets or parentheses
            .replace(Regex("""\s+"""), " ") // Double spaces
            .replace(Regex("""^[-:|/\s]+|[-:|/\s]+$"""), "") // Leading/trailing separators
            .trim()
        
        if (clean.isEmpty()) {
            clean = rawName
        }

        return MediaMetadata(
            cleanName = clean,
            resolution = resolution,
            year = year,
            season = season,
            episode = episode,
            language = language,
            codec = codec,
            isBackupChannel = isBackup
        )
    }

    /**
     * Reorganize channels into refined categories based on formats and metadata.
     */
    fun organizeCategory(groupTitle: String, channelName: String, streamUrl: String): String {
        val metadata = parseMetadata(channelName)
        val streamType = getStreamTypeFromMetadata(groupTitle, channelName, streamUrl)
        
        val cleanGroup = groupTitle.trim()
            .replace(Regex("""(?i)^[\[(]?US[\])]?\s*[-:|/]?\s*"""), "") // Remove US prefixes
            .replace(Regex("""(?i)^[\[(]?UK[\])]?\s*[-:|/]?\s*"""), "") // Remove UK prefixes
            .replace(Regex("""(?i)^[\[(]?CA[\])]?\s*[-:|/]?\s*"""), "") // Remove CA prefixes
            .replace(Regex("""\s+"""), " ")
            .trim()

        return when (streamType) {
            StreamType.MOVIE -> {
                if (metadata.resolution == "4K" || metadata.resolution == "UHD") "4K Movies"
                else if (cleanGroup.contains("movie", ignoreCase = true) || cleanGroup.contains("cinema", ignoreCase = true)) cleanGroup
                else "Movies • $cleanGroup"
            }
            StreamType.TV_SHOW -> {
                if (cleanGroup.contains("series", ignoreCase = true) || cleanGroup.contains("show", ignoreCase = true)) cleanGroup
                else "TV Shows • $cleanGroup"
            }
            StreamType.PPV -> "PPV Events"
            StreamType.LIVE_TV -> {
                if (metadata.isBackupChannel) "Backup Channels"
                else cleanGroup
            }
        }
    }

    private fun getStreamTypeFromMetadata(groupTitle: String, name: String, streamUrl: String): StreamType {
        val group = groupTitle.lowercase()
        val chanName = name.lowercase()
        val url = streamUrl.lowercase()
        
        return when {
            group.contains("ppv") || group.contains("pay-per-view") || group.contains("pay per view") || group.contains("event") || group.contains("box office") || group.contains("boxoffice") ||
            chanName.contains("ppv") || chanName.contains("pay-per-view") || chanName.contains("pay per view") || chanName.contains("ufc") || chanName.contains("wwe") || chanName.contains("boxing") || chanName.contains("event") -> {
                StreamType.PPV
            }
            group.contains("series") || group.contains("tv show") || group.contains("tvshow") || group.contains("shows") || group.contains("season") || group.contains("episode") ||
            chanName.contains("s01") || chanName.contains("s02") || chanName.contains("s03") || chanName.contains("s04") || chanName.contains("s05") || chanName.contains("s06") ||
            url.contains("/series/") || url.contains("/shows/") -> {
                StreamType.TV_SHOW
            }
            group.contains("movie") || group.contains("cinema") || group.contains("film") || group.contains("vod") ||
            chanName.contains("movie") || chanName.contains("cinema") || chanName.contains("film") || chanName.contains("vod") ||
            url.contains("/movie/") || url.endsWith(".mp4") || url.endsWith(".mkv") || url.endsWith(".avi") -> {
                StreamType.MOVIE
            }
            else -> {
                if (url.endsWith(".mp4") || url.endsWith(".mkv") || url.endsWith(".avi") || url.contains("/movie/") || url.contains("/series/")) {
                    StreamType.MOVIE
                } else {
                    StreamType.LIVE_TV
                }
            }
        }
    }
}
