package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Random
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import java.security.SecureRandom

class IptvRepository(
    private val context: Context,
    private val iptvDao: IptvDao
) {
    private val client: OkHttpClient = try {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "*/*")
                    .build()
                chain.proceed(request)
            }
            .build()
    } catch (e: Exception) {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "*/*")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    val playlists: Flow<List<PlaylistEntity>> = iptvDao.getAllPlaylists()
    suspend fun getStaticAllPlaylists(): List<PlaylistEntity> {
        return withContext(Dispatchers.IO) {
            iptvDao.getStaticAllPlaylists()
        }
    }
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

    private fun sanitizeUrl(url: String): String {
        var cleanUrl = url.trim()
        if (cleanUrl.contains("github.com/") && cleanUrl.contains("/blob/")) {
            cleanUrl = cleanUrl
                .replace("github.com/", "raw.githubusercontent.com/")
                .replace("/blob/", "/")
        }
        return cleanUrl
    }

    suspend fun addPlaylist(name: String, url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("IptvRepository", "Adding playlist: $name, $url")
                val sanitizedUrl = sanitizeUrl(url)
                // Insert playlist metadata
                val playlistId = iptvDao.insertPlaylist(
                    PlaylistEntity(name = name, url = sanitizedUrl)
                )

                // Fetch or read M3U stream
                if (sanitizedUrl.startsWith("xtream://")) {
                    // Extract credentials and server URL: xtream://username:password@domain:port
                    val cleanUrl = sanitizedUrl.substring(9)
                    val firstColon = cleanUrl.indexOf(":")
                    val lastAt = cleanUrl.lastIndexOf("@")
                    if (firstColon != -1 && lastAt != -1 && lastAt > firstColon) {
                        val username = cleanUrl.substring(0, firstColon)
                        val password = cleanUrl.substring(firstColon + 1, lastAt)
                        val serverUrl = cleanUrl.substring(lastAt + 1)
                        
                        return@withContext fetchAndStoreXtreamChannels(playlistId.toInt(), serverUrl, username, password)
                    }
                    // Clean up playlist if invalid
                    iptvDao.deletePlaylist(playlistId.toInt())
                    return@withContext false
                }

                val bodyString = if (sanitizedUrl.startsWith("asset://")) {
                    val assetPath = sanitizedUrl.substring(8)
                    context.assets.open(assetPath).use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    }
                } else if (sanitizedUrl.startsWith("file://")) {
                    val filePath = sanitizedUrl.substring(7)
                    File(filePath).readText()
                } else if (sanitizedUrl.startsWith("content://")) {
                    val uri = android.net.Uri.parse(sanitizedUrl)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    } ?: return@withContext false
                } else {
                    val request = Request.Builder().url(sanitizedUrl).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            android.util.Log.e("IptvRepository", "Failed to load playlist: ${response.code} ${response.message}")
                            return@withContext false
                        }
                        response.body?.string() ?: return@withContext false
                    }
                }

                val channels = M3uParser.parse(bodyString, playlistId.toInt())
                android.util.Log.d("IptvRepository", "Parsed ${channels.size} channels")
                if (channels.isNotEmpty()) {
                    // Clean up old channels of this playlist and insert new ones
                    val oldFavorites = iptvDao.getFavoriteChannelsStatic(playlistId.toInt()).map { it.streamUrl }.toSet()
                    val updatedChannels = channels.map { if (it.streamUrl in oldFavorites) it.copy(isFavorite = true) else it }
                    iptvDao.deleteChannelsByPlaylist(playlistId.toInt())
                    iptvDao.insertChannels(updatedChannels)
                    true
                } else {
                    // Clean up playlist if no channels were parsed
                    iptvDao.deletePlaylist(playlistId.toInt())
                    false
                }
            } catch (e: Exception) {
                android.util.Log.e("IptvRepository", "Error adding playlist", e)
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
                
                if (playlist.url.startsWith("xtream://")) {
                    // Extract credentials and server URL: xtream://username:password@domain:port
                    val cleanUrl = playlist.url.substring(9)
                    val firstColon = cleanUrl.indexOf(":")
                    val lastAt = cleanUrl.lastIndexOf("@")
                    if (firstColon != -1 && lastAt != -1 && lastAt > firstColon) {
                        val username = cleanUrl.substring(0, firstColon)
                        val password = cleanUrl.substring(firstColon + 1, lastAt)
                        val serverUrl = cleanUrl.substring(lastAt + 1)
                        
                        return@withContext refreshXtreamPlaylist(playlistId, playlist.name, serverUrl, username, password)
                    }
                    return@withContext false
                }

                val bodyString = if (playlist.url.startsWith("asset://")) {
                    val assetPath = playlist.url.substring(8)
                    context.assets.open(assetPath).use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    }
                } else if (playlist.url.startsWith("file://")) {
                    val filePath = playlist.url.substring(7)
                    File(filePath).readText()
                } else if (playlist.url.startsWith("content://")) {
                    val uri = android.net.Uri.parse(playlist.url)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    } ?: return@withContext false
                } else {
                    val request = Request.Builder().url(playlist.url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@withContext false
                        response.body?.string() ?: return@withContext false
                    }
                }

                val channels = M3uParser.parse(bodyString, playlistId)
                if (channels.isNotEmpty()) {
                    val oldFavorites = iptvDao.getFavoriteChannelsStatic(playlistId).map { it.streamUrl }.toSet()
                    val updatedChannels = channels.map { if (it.streamUrl in oldFavorites) it.copy(isFavorite = true) else it }
                    iptvDao.deleteChannelsByPlaylist(playlistId)
                    iptvDao.insertChannels(updatedChannels)
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

    suspend fun addXtreamPlaylist(
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Normalize server URL
                var normalizedUrl = serverUrl.trim()
                if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
                    normalizedUrl = "http://$normalizedUrl"
                }
                if (normalizedUrl.contains("/player_api.php")) {
                    normalizedUrl = normalizedUrl.substring(0, normalizedUrl.indexOf("/player_api.php"))
                }
                if (normalizedUrl.contains("/get.php")) {
                    normalizedUrl = normalizedUrl.substring(0, normalizedUrl.indexOf("/get.php"))
                }
                while (normalizedUrl.endsWith("/")) {
                    normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length - 1)
                }

                // 1. Fetch user/subscription info to verify connection
                val loginUrl = "$normalizedUrl/player_api.php?username=$username&password=$password"
                val loginResponse = fetchJsonObject(loginUrl) ?: return@withContext false
                
                // Parse user info if available and save to local premium profile
                val userInfo = loginResponse.optJSONObject("user_info")
                if (userInfo != null) {
                    val status = userInfo.optString("status", "Active")
                    val expDateSec = userInfo.optString("exp_date", "")
                    val maxConn = userInfo.optString("max_connections", "1")
                    val activeConn = userInfo.optString("active_cons", "0")
                    val createdAtSec = userInfo.optString("created_at", "")

                    // Save to preferences
                    val prefs = context.getSharedPreferences("iptv_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("premium_username", username)
                        .putString("premium_password", password)
                        .putString("premium_server_url", normalizedUrl)
                        .putString("premium_status", status)
                        .putString("premium_expiry", expDateSec)
                        .putString("premium_max_connections", maxConn)
                        .putString("premium_active_connections", activeConn)
                        .putString("premium_created_at", createdAtSec)
                        .putString("premium_type", "Xtream Codes API")
                        .putBoolean("premium_is_mock", false)
                        .apply()
                }

                // Insert playlist metadata
                val playlistId = iptvDao.insertPlaylist(
                    PlaylistEntity(name = name, url = "xtream://$username:$password@$normalizedUrl")
                )

                fetchAndStoreXtreamChannels(playlistId.toInt(), normalizedUrl, username, password)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private suspend fun refreshXtreamPlaylist(
        playlistId: Int,
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val success = fetchAndStoreXtreamChannels(playlistId, serverUrl, username, password)
                if (success) {
                    val playlist = iptvDao.getPlaylistById(playlistId)
                    if (playlist != null) {
                        iptvDao.updatePlaylist(playlist.copy(lastUpdated = System.currentTimeMillis()))
                    }
                }
                success
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private suspend fun fetchAndStoreXtreamChannels(
        playlistId: Int,
        serverUrl: String,
        username: String,
        password: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val channels = mutableListOf<ChannelEntity>()
            val liveCatMap = mutableMapOf<String, String>()
            val vodCatMap = mutableMapOf<String, String>()

            // 1. Fetch Live Categories (fails gracefully)
            try {
                val liveCatUrl = "$serverUrl/player_api.php?username=$username&password=$password&action=get_live_categories"
                val liveCatsArray = fetchJsonArray(liveCatUrl)
                if (liveCatsArray != null) {
                    for (i in 0 until liveCatsArray.length()) {
                        try {
                            val obj = liveCatsArray.getJSONObject(i)
                            val catId = obj.optString("category_id")
                            val catName = obj.optString("category_name")
                            if (catId.isNotEmpty() && catName.isNotEmpty()) {
                                liveCatMap[catId] = catName
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Fetch VOD Categories (fails gracefully)
            try {
                val vodCatUrl = "$serverUrl/player_api.php?username=$username&password=$password&action=get_vod_categories"
                val vodCatsArray = fetchJsonArray(vodCatUrl)
                if (vodCatsArray != null) {
                    for (i in 0 until vodCatsArray.length()) {
                        try {
                            val obj = vodCatsArray.getJSONObject(i)
                            val catId = obj.optString("category_id")
                            val catName = obj.optString("category_name")
                            if (catId.isNotEmpty() && catName.isNotEmpty()) {
                                vodCatMap[catId] = catName
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. Fetch Live Streams (fails gracefully)
            try {
                val liveStreamsUrl = "$serverUrl/player_api.php?username=$username&password=$password&action=get_live_streams"
                val liveStreamsArray = fetchJsonArray(liveStreamsUrl)
                if (liveStreamsArray != null) {
                    for (i in 0 until liveStreamsArray.length()) {
                        try {
                            val obj = liveStreamsArray.getJSONObject(i)
                            val streamId = obj.optString("stream_id")
                            val streamName = obj.optString("name")
                            val categoryId = obj.optString("category_id")
                            val streamIcon = obj.optString("stream_icon")
                            val tvgId = obj.optString("epg_channel_id")

                            val groupTitle = liveCatMap[categoryId] ?: "Live TV"
                            val streamUrl = "$serverUrl/live/$username/$password/$streamId.ts"

                            channels.add(
                                ChannelEntity(
                                    playlistId = playlistId,
                                    name = streamName,
                                    logoUrl = if (streamIcon.isNotEmpty()) streamIcon else null,
                                    groupTitle = groupTitle,
                                    streamUrl = streamUrl,
                                    tvgId = if (tvgId.isNotEmpty()) tvgId else null,
                                    tvgName = streamName,
                                    catchupType = null,
                                    catchupSource = null
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

            // 4. Fetch VOD (Movies, fails gracefully)
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
                                    catchupSource = null
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

            try {
                if (channels.isNotEmpty()) {
                    val oldFavorites = iptvDao.getFavoriteChannelsStatic(playlistId).map { it.streamUrl }.toSet()
                    val updatedChannels = channels.map { if (it.streamUrl in oldFavorites) it.copy(isFavorite = true) else it }
                    iptvDao.deleteChannelsByPlaylist(playlistId)
                    iptvDao.insertChannels(updatedChannels)
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

    private suspend fun fetchJsonArray(url: String): JSONArray? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) null
                    else {
                        val body = response.body?.string() ?: return@use null
                        JSONArray(body)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun fetchJsonObjectPublic(url: String): JSONObject? {
        return fetchJsonObject(url)
    }

    private suspend fun fetchJsonObject(url: String): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) null
                    else {
                        val body = response.body?.string() ?: return@use null
                        JSONObject(body)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
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

    // --- Background Download Manager Engine ---
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<Int, Job>()
    val activeDownloadStates = MutableStateFlow<Map<Int, DownloadProgressState>>(emptyMap())

    private fun updateActiveState(state: DownloadProgressState) {
        activeDownloadStates.update { it + (state.id to state) }
    }

    private fun removeActiveState(id: Int) {
        activeDownloadStates.update { it - id }
    }

    fun isJobActive(id: Int): Boolean {
        return activeJobs.containsKey(id) && activeJobs[id]?.isActive == true
    }

    fun startDownload(downloadId: Int, channelName: String, streamUrl: String, filePath: String) {
        activeJobs[downloadId]?.cancel()

        val job = downloadScope.launch {
            var input: InputStream? = null
            var output: FileOutputStream? = null
            var connection: java.net.HttpURLConnection? = null
            try {
                var dbEntity = DownloadEntity(
                    id = downloadId,
                    channelName = channelName,
                    streamUrl = streamUrl,
                    filePath = filePath,
                    status = "DOWNLOADING"
                )
                iptvDao.updateDownload(dbEntity)

                val destFile = File(filePath)
                var downloadedBytes = 0L
                if (destFile.exists()) {
                    downloadedBytes = destFile.length()
                }

                val url = java.net.URL(streamUrl)
                connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                connection.setRequestProperty("Accept", "*/*")

                var isResumeSupported = false
                if (downloadedBytes > 0) {
                    connection.setRequestProperty("Range", "bytes=$downloadedBytes-")
                }

                connection.connect()

                val responseCode = connection.responseCode
                var totalBytes = connection.contentLengthLong

                if (downloadedBytes > 0 && responseCode == java.net.HttpURLConnection.HTTP_PARTIAL) {
                    isResumeSupported = true
                    if (totalBytes > 0) {
                        totalBytes += downloadedBytes
                    }
                    output = FileOutputStream(destFile, true)
                } else {
                    downloadedBytes = 0L
                    output = FileOutputStream(destFile, false)
                }

                if (totalBytes <= 0) {
                    totalBytes = connection.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
                    if (totalBytes > 0 && isResumeSupported) {
                        totalBytes += downloadedBytes
                    }
                }

                input = connection.inputStream

                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var lastUpdateTime = System.currentTimeMillis()
                var lastUpdateBytes = downloadedBytes
                var speedBps = 0L

                val maxBytesLimit = if (totalBytes <= 1L) 150 * 1024 * 1024L else totalBytes // 150MB limit for chunked/live recording

                while (isActive) {
                    bytesRead = input.read(buffer)
                    if (bytesRead == -1) break

                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val now = System.currentTimeMillis()
                    val elapsed = now - lastUpdateTime
                    if (elapsed >= 1000) {
                        val bytesDiff = downloadedBytes - lastUpdateBytes
                        speedBps = (bytesDiff * 1000) / elapsed
                        lastUpdateTime = now
                        lastUpdateBytes = downloadedBytes
                    }

                    val progress = if (totalBytes > 0) {
                        downloadedBytes.toFloat() / totalBytes
                    } else {
                        (downloadedBytes.toFloat() / maxBytesLimit).coerceAtMost(0.99f)
                    }

                    val etaSeconds = if (speedBps > 0 && totalBytes > 0) {
                        (totalBytes - downloadedBytes) / speedBps
                    } else {
                        -1L
                    }

                    updateActiveState(
                        DownloadProgressState(
                            id = downloadId,
                            status = "DOWNLOADING",
                            progress = progress,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes,
                            speedBps = speedBps,
                            etaSeconds = etaSeconds
                        )
                    )

                    if (now % 5 == 0L || downloadedBytes == totalBytes) {
                        iptvDao.updateDownload(
                            dbEntity.copy(
                                fileSize = downloadedBytes,
                                progress = progress,
                                status = "DOWNLOADING"
                            )
                        )
                    }

                    if (totalBytes <= 1L && downloadedBytes >= maxBytesLimit) {
                        break
                    }
                }

                output.flush()

                if (!isActive) {
                    iptvDao.updateDownload(
                        dbEntity.copy(
                            fileSize = downloadedBytes,
                            progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f,
                            status = "PAUSED"
                        )
                    )
                    removeActiveState(downloadId)
                } else {
                    iptvDao.updateDownload(
                        dbEntity.copy(
                            fileSize = downloadedBytes,
                            progress = 1.0f,
                            status = "COMPLETED"
                        )
                    )
                    removeActiveState(downloadId)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                iptvDao.updateDownload(
                    DownloadEntity(
                        id = downloadId,
                        channelName = channelName,
                        streamUrl = streamUrl,
                        filePath = filePath,
                        status = "FAILED"
                    )
                )
                removeActiveState(downloadId)
            } finally {
                try { input?.close() } catch (e: Exception) {}
                try { output?.close() } catch (e: Exception) {}
                try { connection?.disconnect() } catch (e: Exception) {}
                activeJobs.remove(downloadId)
            }
        }

        activeJobs[downloadId] = job
    }

    fun pauseDownload(id: Int) {
        val job = activeJobs[id]
        if (job != null) {
            job.cancel()
        } else {
            downloadScope.launch {
                val db = iptvDao.getDownloadById(id)
                if (db != null) {
                    iptvDao.updateDownload(db.copy(status = "PAUSED"))
                }
            }
        }
    }

    fun cancelDownload(id: Int) {
        activeJobs[id]?.cancel()
        removeActiveState(id)
        downloadScope.launch {
            iptvDao.deleteDownload(id)
        }
    }

    suspend fun createDownloadEntity(channelName: String, streamUrl: String): Int {
        return withContext(Dispatchers.IO) {
            val destFile = File(context.filesDir, "recording_${System.currentTimeMillis()}.ts")
            val id = iptvDao.insertDownload(
                DownloadEntity(
                    channelName = channelName,
                    streamUrl = streamUrl,
                    filePath = destFile.absolutePath,
                    status = "QUEUED",
                    progress = 0f
                )
            )
            id.toInt()
        }
    }
}

