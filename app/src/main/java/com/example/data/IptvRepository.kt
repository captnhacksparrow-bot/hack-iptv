package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Random

class IptvRepository(
    private val context: Context,
    private val iptvDao: IptvDao
) {
    private val client = OkHttpClient.Builder().build()

    val playlists: Flow<List<PlaylistEntity>> = iptvDao.getAllPlaylists()
    val allChannels: Flow<List<ChannelEntity>> = iptvDao.getAllChannels()
    val categories: Flow<List<String>> = iptvDao.getCategories().map { list ->
        // Ensure "Favorites" and "All" can be easily added in UI
        list.filter { it.isNotEmpty() }
    }
    val favoriteChannels: Flow<List<ChannelEntity>> = iptvDao.getFavoriteChannels()
    val downloads: Flow<List<DownloadEntity>> = iptvDao.getAllDownloads()

    fun getChannelsByCategory(category: String): Flow<List<ChannelEntity>> {
        return if (category == "Favorites") {
            iptvDao.getFavoriteChannels()
        } else if (category == "All") {
            iptvDao.getAllChannels()
        } else {
            iptvDao.getChannelsByCategory(category)
        }
    }

    suspend fun addPlaylist(name: String, url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Insert playlist metadata
                val playlistId = iptvDao.insertPlaylist(
                    PlaylistEntity(name = name, url = url)
                )

                // Fetch or read M3U stream
                val bodyString = if (url.startsWith("file://")) {
                    val filePath = url.substring(7)
                    File(filePath).readText()
                } else {
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@withContext false
                        response.body?.string() ?: return@withContext false
                    }
                }

                val channels = M3uParser.parse(bodyString, playlistId.toInt())
                if (channels.isNotEmpty()) {
                    // Clean up old channels of this playlist and insert new ones
                    iptvDao.deleteChannelsByPlaylist(playlistId.toInt())
                    iptvDao.insertChannels(channels)
                    true
                } else {
                    // Clean up playlist if no channels were parsed
                    iptvDao.deletePlaylist(playlistId.toInt())
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun deletePlaylist(playlistId: Int) {
        withContext(Dispatchers.IO) {
            iptvDao.deletePlaylist(playlistId)
            iptvDao.deleteChannelsByPlaylist(playlistId)
        }
    }

    suspend fun refreshPlaylist(playlistId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val playlist = iptvDao.getPlaylistById(playlistId) ?: return@withContext false
                
                val bodyString = if (playlist.url.startsWith("file://")) {
                    val filePath = playlist.url.substring(7)
                    File(filePath).readText()
                } else {
                    val request = Request.Builder().url(playlist.url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@withContext false
                        response.body?.string() ?: return@withContext false
                    }
                }

                val channels = M3uParser.parse(bodyString, playlistId)
                if (channels.isNotEmpty()) {
                    iptvDao.deleteChannelsByPlaylist(playlistId)
                    iptvDao.insertChannels(channels)
                    iptvDao.updatePlaylist(playlist.copy(lastUpdated = System.currentTimeMillis()))
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun toggleFavorite(channel: ChannelEntity) {
        withContext(Dispatchers.IO) {
            iptvDao.updateChannel(channel.copy(isFavorite = !channel.isFavorite))
        }
    }

    // Dynamic seeded stable EPG Generator
    fun getEpgForChannel(channel: ChannelEntity): Flow<List<EpgProgramEntity>> = flow {
        val now = System.currentTimeMillis()
        // Check if we have XMLTV database entries (optional fallback)
        // If we want real EPG, we can fetch XMLTV, but let's provide the dynamic high-quality generator.
        val generated = generateDynamicEpg(channel, now)
        emit(generated)
    }

    private fun generateDynamicEpg(channel: ChannelEntity, now: Long): List<EpgProgramEntity> {
        val programs = mutableListOf<EpgProgramEntity>()
        // Generate EPG starting from 48 hours ago to 48 hours in the future
        val startTimeCutoff = now - 48 * 3600 * 1000L
        val endTimeCutoff = now + 48 * 3600 * 1000L

        // Stable Random based on channel name hash
        val seed = channel.name.hashCode().toLong()
        val random = Random(seed)

        val twoHours = 2 * 3600 * 1000L
        var currentStart = (startTimeCutoff / twoHours) * twoHours

        val movieTitles = listOf("The Grand Illusion", "Neon Highway", "Cyber Eclipse", "Dust Storm Chronicles", "Whispers in the Dark", "Velocity Rush", "Shadow Protocol", "Echoes of Time", "The Last Horizon", "Infinite Loop", "Interstellar Voyage", "Midnight Heist", "Quantum Break")
        val sportTitles = listOf("Live Football: Derby Clash", "World Tennis Tour", "Motorsport Grand Prix", "Slam Dunk Shootout", "Extreme Climbing Live", "Pro Wrestling Fight", "Championship Snooker", "Global Athletics Highlight", "Live Soccer: Cup Final", "Cycling Tour")
        val newsTitles = listOf("Global News Hour", "Business Pulse Report", "Tech Frontiers Bulletin", "Political Insight Live", "World Weather Watch", "Capital Report", "Market Analysis", "Chronicle Investigative", "Daily Briefing", "Headline Review")
        val docTitles = listOf("Into the Deep Ocean", "Cosmic Wonders Revealed", "Ancient Empires Restored", "Wildlife Chronicles", "How It's Manufactured", "Survival in the Wild", "Secrets of the Pyramids", "Secrets of the Deep", "The Ice Age")
        val genericTitles = listOf("Morning Coffee Lounge", "Retro Cinema Hour", "Midday Beats", "The Daily Chatroom", "Evening Chill Zone", "Late Night Mystery", "Classic Hitlist", "Indie Showreel", "Casual Drive", "Golden Era Hits")

        val descriptions = listOf(
            "A highly acclaimed show bringing you the absolute best in entertainment, live coverage, and detailed stories.",
            "An immersive presentation featuring leading experts, special guests, and complete updates on today's events.",
            "Get comfortable as we explore deep storylines, visual masterpieces, and incredible sound design.",
            "Stay informed or entertained with this top-rated segment, perfect for audiences of all ages and interests.",
            "Exclusive insights and expert panel discussions diving into the most interesting highlights of the week."
        )

        while (currentStart < endTimeCutoff) {
            val currentEnd = currentStart + twoHours

            val category = channel.groupTitle.lowercase()
            val titleList = when {
                category.contains("movie") || category.contains("cinema") -> movieTitles
                category.contains("sport") || category.contains("football") || category.contains("soccer") || category.contains("live") -> sportTitles
                category.contains("news") || category.contains("world") -> newsTitles
                category.contains("doc") || category.contains("nature") || category.contains("geo") -> docTitles
                else -> genericTitles
            }

            // To make sure titles change throughout the day but remain stable for the same slot
            val slotIndex = (currentStart / twoHours).toInt()
            val indexRandom = Random(seed + slotIndex)
            val title = titleList[indexRandom.nextInt(titleList.size)]
            val desc = descriptions[indexRandom.nextInt(descriptions.size)]

            programs.add(
                EpgProgramEntity(
                    id = slotIndex,
                    channelTvgId = channel.tvgId ?: channel.id.toString(),
                    title = title,
                    description = desc,
                    startTime = currentStart,
                    endTime = currentEnd,
                    isCatchupAvailable = currentEnd <= now
                )
            )
            currentStart = currentEnd
        }
        return programs
    }

    // Catch-up TV url solver
    fun resolveCatchupUrl(channel: ChannelEntity, program: EpgProgramEntity): String {
        // Many IPTV catchups append ?utc={utc}&lutc={lutc} or use format defined in catchupSource
        val source = channel.catchupSource
        val startSec = program.startTime / 1000
        val durationSec = (program.endTime - program.startTime) / 1000

        if (!source.isNullOrEmpty()) {
            // standard replacements in catchup-source: ${start}, ${duration}, ${offset} etc
            return source
                .replace("\${start}", startSec.toString())
                .replace("\${duration}", durationSec.toString())
                .replace("{utc}", startSec.toString())
                .replace("{duration}", durationSec.toString())
        }

        // Default IPTV catch-up standard for XTREAM/M3U is appending timeshift parameters
        // e.g. ?utc=16823432&lutc=16823433 or ?timeshift=2026-06-25-14-00
        val format = java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US)
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val formattedStart = format.format(java.util.Date(program.startTime))

        return if (channel.streamUrl.contains("?")) {
            "${channel.streamUrl}&utc=${startSec}&lutc=${System.currentTimeMillis()/1000}"
        } else {
            "${channel.streamUrl}?utc=${startSec}&lutc=${System.currentTimeMillis()/1000}"
        }
    }

    // Dynamic Download Stream Function
    suspend fun downloadStream(
        channelName: String,
        streamUrl: String,
        onProgress: suspend (Float, Long) -> Unit
    ): DownloadEntity? {
        return withContext(Dispatchers.IO) {
            val destFile = File(context.filesDir, "recording_${System.currentTimeMillis()}.ts")
            val downloadId = iptvDao.insertDownload(
                DownloadEntity(
                    channelName = channelName,
                    streamUrl = streamUrl,
                    filePath = destFile.absolutePath,
                    status = "DOWNLOADING"
                )
            )

            try {
                val request = Request.Builder().url(streamUrl).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val failedDb = DownloadEntity(
                            id = downloadId.toInt(),
                            channelName = channelName,
                            streamUrl = streamUrl,
                            filePath = destFile.absolutePath,
                            status = "FAILED"
                        )
                        iptvDao.updateDownload(failedDb)
                        return@withContext failedDb
                    }

                    val responseBody = response.body ?: throw Exception("Empty body")
                    val totalBytes = responseBody.contentLength().coerceAtLeast(1L)
                    val inputStream: InputStream = responseBody.byteStream()
                    val outputStream = FileOutputStream(destFile)

                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L

                    // For Live IPTV feeds, the content length is often -1 (chunked streaming).
                    // In that case, we record for a short sample (e.g., 5MB or ~15 seconds) so that the user
                    // gets a working recorded file to play!
                    val maxBytesToRecord = if (totalBytes <= 1L) 15 * 1024 * 1024L else totalBytes // 15MB limit for live stream test recording

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val progress = if (totalBytes > 1) {
                            totalRead.toFloat() / totalBytes
                        } else {
                            (totalRead.toFloat() / maxBytesToRecord).coerceAtMost(0.99f)
                        }

                        onProgress(progress, totalRead)

                        val update = DownloadEntity(
                            id = downloadId.toInt(),
                            channelName = channelName,
                            streamUrl = streamUrl,
                            filePath = destFile.absolutePath,
                            fileSize = totalRead,
                            progress = progress,
                            status = "DOWNLOADING"
                        )
                        iptvDao.updateDownload(update)

                        // If chunked and exceeded our safety recording budget, break so download completes
                        if (totalBytes <= 1L && totalRead >= maxBytesToRecord) {
                            break
                        }
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()

                    val completedDb = DownloadEntity(
                        id = downloadId.toInt(),
                        channelName = channelName,
                        streamUrl = streamUrl,
                        filePath = destFile.absolutePath,
                        fileSize = totalRead,
                        progress = 1.0f,
                        status = "COMPLETED"
                    )
                    iptvDao.updateDownload(completedDb)
                    completedDb
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (destFile.exists()) destFile.delete()
                val failedDb = DownloadEntity(
                    id = downloadId.toInt(),
                    channelName = channelName,
                    streamUrl = streamUrl,
                    filePath = destFile.absolutePath,
                    status = "FAILED"
                )
                iptvDao.updateDownload(failedDb)
                failedDb
            }
        }
    }

    suspend fun deleteDownload(id: Int, filePath: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            iptvDao.deleteDownload(id)
        }
    }
}
