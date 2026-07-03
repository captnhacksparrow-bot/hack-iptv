package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playlistId: Int,
    val name: String,
    val logoUrl: String?,
    val groupTitle: String, // Category name parsed from group-title
    val streamUrl: String,
    val tvgId: String?,
    val tvgName: String?,
    val catchupType: String?,   // e.g. "default"
    val catchupDays: Int = 0,   // number of days available
    val catchupSource: String?, // append query template
    val isFavorite: Boolean = false,
    val country: String = "Other"
)

@Entity(tableName = "epg_programs")
data class EpgProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val channelTvgId: String,
    val title: String,
    val description: String?,
    val startTime: Long, // Epoch timestamp (milliseconds)
    val endTime: Long,   // Epoch timestamp (milliseconds)
    val isCatchupAvailable: Boolean = false
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val channelName: String,
    val streamUrl: String,
    val filePath: String,
    val downloadTime: Long = System.currentTimeMillis(),
    val fileSize: Long = 0,
    val durationSec: Long = 0,
    val progress: Float = 0f, // 0.0 to 1.0
    val status: String // "DOWNLOADING", "COMPLETED", "FAILED"
)

enum class StreamType {
    LIVE_TV,
    PPV,
    MOVIE,
    TV_SHOW
}

fun ChannelEntity.getStreamType(): StreamType {
    val group = groupTitle.lowercase()
    val chanName = name.lowercase()
    val url = streamUrl.lowercase()
    
    return when {
        // PPV / Live Events / Sports events
        group.contains("ppv") || group.contains("pay-per-view") || group.contains("pay per view") || group.contains("event") || group.contains("box office") || group.contains("boxoffice") ||
        chanName.contains("ppv") || chanName.contains("pay-per-view") || chanName.contains("pay per view") || chanName.contains("ufc") || chanName.contains("wwe") || chanName.contains("boxing") || chanName.contains("event") -> {
            StreamType.PPV
        }
        // TV Shows / Series (On Demand)
        group.contains("series") || group.contains("tv show") || group.contains("tvshow") || group.contains("shows") || group.contains("season") || group.contains("episode") ||
        chanName.contains("s01") || chanName.contains("s02") || chanName.contains("s03") || chanName.contains("s04") || chanName.contains("s05") || chanName.contains("s06") ||
        url.contains("/series/") || url.contains("/shows/") -> {
            StreamType.TV_SHOW
        }
        // Movies (On Demand)
        group.contains("movie") || group.contains("cinema") || group.contains("film") || group.contains("vod") ||
        chanName.contains("movie") || chanName.contains("cinema") || chanName.contains("film") || chanName.contains("vod") ||
        url.contains("/movie/") || url.endsWith(".mp4") || url.endsWith(".mkv") || url.endsWith(".avi") -> {
            StreamType.MOVIE
        }
        else -> {
            // Check extension of stream URL for media file formats (VOD / On Demand)
            if (url.endsWith(".mp4") || url.endsWith(".mkv") || url.endsWith(".avi") || url.contains("/movie/") || url.contains("/series/")) {
                StreamType.MOVIE
            } else {
                StreamType.LIVE_TV
            }
        }
    }
}

data class DownloadProgressState(
    val id: Int,
    val status: String,
    val progress: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBps: Long,
    val etaSeconds: Long
)


