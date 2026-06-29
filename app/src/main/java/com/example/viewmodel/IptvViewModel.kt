package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class IptvViewModel(
    application: Application,
    private val repository: IptvRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("iptv_prefs", Context.MODE_PRIVATE)

    // Device Mode: "TV" or "MOBILE" (or null if not selected yet)
    private val _deviceMode = MutableStateFlow<String?>(prefs.getString("device_mode", null))
    val deviceMode: StateFlow<String?> = _deviceMode.asStateFlow()

    private val _tvIpAddress = MutableStateFlow<String>(prefs.getString("tv_ip_address", "") ?: "")
    val tvIpAddress: StateFlow<String> = _tvIpAddress.asStateFlow()

    private val _isTvConnected = MutableStateFlow(false)
    val isTvConnected: StateFlow<Boolean> = _isTvConnected.asStateFlow()

    // Premium IPTV Profile states
    private val _premiumUsername = MutableStateFlow(prefs.getString("premium_username", "") ?: "")
    val premiumUsername: StateFlow<String> = _premiumUsername.asStateFlow()

    private val _premiumPassword = MutableStateFlow(prefs.getString("premium_password", "") ?: "")
    val premiumPassword: StateFlow<String> = _premiumPassword.asStateFlow()

    private val _premiumServerUrl = MutableStateFlow(prefs.getString("premium_server_url", "") ?: "")
    val premiumServerUrl: StateFlow<String> = _premiumServerUrl.asStateFlow()

    private val _premiumStatus = MutableStateFlow(prefs.getString("premium_status", "") ?: "")
    val premiumStatus: StateFlow<String> = _premiumStatus.asStateFlow()

    private val _premiumExpiry = MutableStateFlow(prefs.getString("premium_expiry", "") ?: "")
    val premiumExpiry: StateFlow<String> = _premiumExpiry.asStateFlow()

    private val _premiumMaxConnections = MutableStateFlow(prefs.getString("premium_max_connections", "1") ?: "1")
    val premiumMaxConnections: StateFlow<String> = _premiumMaxConnections.asStateFlow()

    private val _premiumActiveConnections = MutableStateFlow(prefs.getString("premium_active_connections", "0") ?: "0")
    val premiumActiveConnections: StateFlow<String> = _premiumActiveConnections.asStateFlow()

    private val _premiumCreatedAt = MutableStateFlow(prefs.getString("premium_created_at", "") ?: "")
    val premiumCreatedAt: StateFlow<String> = _premiumCreatedAt.asStateFlow()

    private val _premiumType = MutableStateFlow(prefs.getString("premium_type", "") ?: "")
    val premiumType: StateFlow<String> = _premiumType.asStateFlow()

    private val _premiumIsMock = MutableStateFlow(prefs.getBoolean("premium_is_mock", false))
    val premiumIsMock: StateFlow<Boolean> = _premiumIsMock.asStateFlow()

    // Custom user preference states
    private val _avatarUrl = MutableStateFlow<String>(prefs.getString("avatar_url", "") ?: "")
    val avatarUrl: StateFlow<String> = _avatarUrl.asStateFlow()

    private val _appTheme = MutableStateFlow<String>(prefs.getString("app_theme", "Gold") ?: "Gold")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _autoplayNext = MutableStateFlow<Boolean>(prefs.getBoolean("autoplay_next", false))
    val autoplayNext: StateFlow<Boolean> = _autoplayNext.asStateFlow()

    private val _autoFullscreen = MutableStateFlow<Boolean>(prefs.getBoolean("auto_fullscreen", false))
    val autoFullscreen: StateFlow<Boolean> = _autoFullscreen.asStateFlow()

    private val _showTmdb = MutableStateFlow<Boolean>(prefs.getBoolean("show_tmdb", true))
    val showTmdb: StateFlow<Boolean> = _showTmdb.asStateFlow()

    // 6-digit PIN state for TV screen pairing
    private val _pairingCode = MutableStateFlow<String>("")
    val pairingCode: StateFlow<String> = _pairingCode.asStateFlow()

    // Playlists UI State
    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expose all channels for profile stats
    val allChannels: StateFlow<List<ChannelEntity>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Playlist ID (Active Playlist filtering)
    private val _selectedPlaylistId = MutableStateFlow<Int?>(null)
    val selectedPlaylistId: StateFlow<Int?> = _selectedPlaylistId.asStateFlow()

    // Playlist Selector on Start Control
    private val _showPlaylistSelectorOnStart = MutableStateFlow(false)
    val showPlaylistSelectorOnStart: StateFlow<Boolean> = _showPlaylistSelectorOnStart.asStateFlow()

    // Category Navigation State
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Search filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Stream Type Selection State (Live TV, Movie, TV Show)
    private val _selectedStreamType = MutableStateFlow(StreamType.LIVE_TV)
    val selectedStreamType: StateFlow<StreamType> = _selectedStreamType.asStateFlow()

    // Playlist refresh status flow
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Dynamic combined list of Categories (Favorites, All, + parsed groups) filtered by StreamType and Selected Playlist
    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<String>> = combine(
        repository.allChannels,
        _selectedStreamType,
        _selectedPlaylistId
    ) { allChans, streamType, playlistId ->
        val filteredByPlaylist = if (playlistId != null) {
            allChans.filter { it.playlistId == playlistId }
        } else {
            allChans
        }
        val groups = filteredByPlaylist
            .filter { it.getStreamType() == streamType }
            .map { it.groupTitle }
            .distinct()
            .filter { it.isNotEmpty() }
            .sorted()
        listOf("All", "Favorites") + groups
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All", "Favorites"))

    // Dynamic counts of channels for each Category
    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryCounts: StateFlow<Map<String, Int>> = combine(
        repository.allChannels,
        _selectedStreamType,
        _selectedPlaylistId
    ) { allChans, streamType, playlistId ->
        val filteredByPlaylist = if (playlistId != null) {
            allChans.filter { it.playlistId == playlistId }
        } else {
            allChans
        }
        val filteredByType = filteredByPlaylist.filter { it.getStreamType() == streamType }
        
        val counts = mutableMapOf<String, Int>()
        counts["All"] = filteredByType.size
        counts["Favorites"] = filteredByType.count { it.isFavorite }
        
        filteredByType.forEach { channel ->
            val group = channel.groupTitle
            if (group.isNotEmpty()) {
                counts[group] = (counts[group] ?: 0) + 1
            }
        }
        counts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Channel Filtering State based on category, query, streamType, and Selected Playlist
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredChannels: StateFlow<List<ChannelEntity>> = combine(
        repository.allChannels,
        _selectedCategory,
        _searchQuery,
        _selectedStreamType,
        _selectedPlaylistId
    ) { allChans, category, query, streamType, playlistId ->
        var list = allChans
        
        // 1. Filter by playlist
        if (playlistId != null) {
            list = list.filter { it.playlistId == playlistId }
        }
        
        // 2. Filter by category
        list = when (category) {
            "All" -> list
            "Favorites" -> list.filter { it.isFavorite }
            else -> list.filter { it.groupTitle == category }
        }
        
        // 3. Filter by stream type
        list = list.filter { it.getStreamType() == streamType }
        
        // 4. Filter by search query
        if (query.isNotEmpty()) {
            list = list.filter { it.name.contains(query, ignoreCase = true) }
        }
        
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Playback State
    private val _selectedChannel = MutableStateFlow<ChannelEntity?>(null)
    val selectedChannel: StateFlow<ChannelEntity?> = _selectedChannel.asStateFlow()

    private val _activePlayUrl = MutableStateFlow<String?>(null)
    val activePlayUrl: StateFlow<String?> = _activePlayUrl.asStateFlow()

    private val _isPlayingCatchup = MutableStateFlow(false)
    val isPlayingCatchup: StateFlow<Boolean> = _isPlayingCatchup.asStateFlow()

    private val _selectedProgram = MutableStateFlow<EpgProgramEntity?>(null)
    val selectedProgram: StateFlow<EpgProgramEntity?> = _selectedProgram.asStateFlow()

    // Dynamic EPG programs for the currently selected channel
    @OptIn(ExperimentalCoroutinesApi::class)
    val epgPrograms: StateFlow<List<EpgProgramEntity>> = _selectedChannel
        .flatMapLatest { channel ->
            if (channel != null) {
                repository.getEpgForChannel(channel)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Downloads list state
    val downloads: StateFlow<List<DownloadEntity>> = repository.downloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Loading state variables
    private val _isAddingPlaylist = MutableStateFlow(false)
    val isAddingPlaylist: StateFlow<Boolean> = _isAddingPlaylist.asStateFlow()

    private val _playlistAddError = MutableStateFlow<String?>(null)
    val playlistAddError: StateFlow<String?> = _playlistAddError.asStateFlow()

    // Download Active Progress Monitor (StreamUrl -> progress ratio)
    private val _downloadProgresses = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgresses: StateFlow<Map<String, Float>> = _downloadProgresses.asStateFlow()

    val activeDownloadStates: StateFlow<Map<Int, DownloadProgressState>> = repository.activeDownloadStates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    init {
        // Automatically start remote server if TV mode is active
        if (_deviceMode.value == "TV") {
            startRemoteServer()
        }

        // Pre-populate with free list index and clean up old premium/VOD lists if present
        viewModelScope.launch {
            try {
                val currentPlaylists = repository.getStaticAllPlaylists()
                
                // Clear old custom premium playlists/VODs
                currentPlaylists.forEach { playlist ->
                    if (playlist.name.contains("Capt'n Hack Premium") || playlist.name.contains("VOD & PPV")) {
                        repository.deletePlaylist(playlist.id)
                    }
                }

                // Decide which playlist to load on start
                val finalPlaylists = repository.getStaticAllPlaylists()
                if (finalPlaylists.size > 1) {
                    _showPlaylistSelectorOnStart.value = true
                } else if (finalPlaylists.size == 1) {
                    _selectedPlaylistId.value = finalPlaylists.first().id
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isAddingPlaylist.value = false
            }
        }

        // Initial refresh check
        viewModelScope.launch {
            val lastRefresh = prefs.getLong("last_refresh_time", 0L)
            val now = System.currentTimeMillis()
            val twoDays = 2 * 24 * 60 * 60 * 1000L
            if (lastRefresh == 0L || now - lastRefresh > twoDays) {
                refreshAllPlaylists()
            }
        }

        // Periodic background refresh (every 1 hour) to keep the channel lists current
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(3600 * 1000L) // Wait 1 hour
                try {
                    refreshAllPlaylists()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Automatically set first channel to play if channels exist
        viewModelScope.launch {
            filteredChannels.collect { channels ->
                if (_selectedChannel.value == null && channels.isNotEmpty()) {
                    selectChannel(channels.first())
                }
            }
        }
    }

    // Action Methods
    fun selectPlaylist(playlistId: Int?) {
        _selectedPlaylistId.value = playlistId
        _selectedChannel.value = null
        _activePlayUrl.value = null
        _isPlayingCatchup.value = false
        _selectedProgram.value = null
        _selectedCategory.value = "All"
    }

    fun loadPlaylistOnStart(playlistId: Int) {
        _selectedPlaylistId.value = playlistId
        _showPlaylistSelectorOnStart.value = false
    }

    fun selectStreamType(streamType: StreamType) {
        _selectedStreamType.value = streamType
        _selectedCategory.value = "All"
    }

    fun refreshPlaylist(playlistId: Int) {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshPlaylist(playlistId)
            _isRefreshing.value = false
        }
    }

    fun refreshAllPlaylists() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val currentPlaylists = repository.getStaticAllPlaylists()
                for (playlist in currentPlaylists) {
                    repository.refreshPlaylist(playlist.id)
                }
                prefs.edit().putLong("last_refresh_time", System.currentTimeMillis()).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectChannel(channel: ChannelEntity) {
        _selectedChannel.value = channel
        _activePlayUrl.value = channel.streamUrl
        _isPlayingCatchup.value = false
        _selectedProgram.value = null
    }

    fun playCatchupProgram(program: EpgProgramEntity) {
        val channel = _selectedChannel.value ?: return
        val resolvedUrl = repository.resolveCatchupUrl(channel, program)
        _activePlayUrl.value = resolvedUrl
        _isPlayingCatchup.value = true
        _selectedProgram.value = program
    }

    fun toggleFavorite(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(channel)
        }
    }

    fun addPlaylist(name: String, url: String) {
        viewModelScope.launch {
            _isAddingPlaylist.value = true
            _playlistAddError.value = null
            val success = repository.addPlaylist(name, url)
            _isAddingPlaylist.value = false
            if (success) {
                val allPlaylists = repository.getStaticAllPlaylists()
                val added = allPlaylists.find { it.url == url }
                if (added != null) {
                    selectPlaylist(added.id)
                }
            } else {
                _playlistAddError.value = "Failed to parse playlist. Check URL."
            }
        }
    }

    fun addXtreamPlaylist(name: String, serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _isAddingPlaylist.value = true
            _playlistAddError.value = null
            val success = repository.addXtreamPlaylist(name, serverUrl, username, password)
            _isAddingPlaylist.value = false
            if (success) {
                // Reload preferences to stateflows
                _premiumUsername.value = prefs.getString("premium_username", "") ?: ""
                _premiumPassword.value = prefs.getString("premium_password", "") ?: ""
                _premiumServerUrl.value = prefs.getString("premium_server_url", "") ?: ""
                _premiumStatus.value = prefs.getString("premium_status", "") ?: ""
                _premiumExpiry.value = prefs.getString("premium_expiry", "") ?: ""
                _premiumMaxConnections.value = prefs.getString("premium_max_connections", "1") ?: "1"
                _premiumActiveConnections.value = prefs.getString("premium_active_connections", "0") ?: "0"
                _premiumCreatedAt.value = prefs.getString("premium_created_at", "") ?: ""
                _premiumType.value = prefs.getString("premium_type", "") ?: ""
                _premiumIsMock.value = prefs.getBoolean("premium_is_mock", false)

                val allPlaylists = repository.getStaticAllPlaylists()
                val added = allPlaylists.find { it.url.startsWith("xtream://$username") }
                if (added != null) {
                    selectPlaylist(added.id)
                }
            } else {
                _playlistAddError.value = "Failed to connect or fetch from Xtream server. Verify credentials & connection."
            }
        }
    }

    fun updatePremiumProfile(
        username: String,
        password: String,
        serverUrl: String,
        status: String,
        expiry: String,
        maxConn: String,
        activeConn: String,
        type: String,
        isMock: Boolean
    ) {
        prefs.edit()
            .putString("premium_username", username)
            .putString("premium_password", password)
            .putString("premium_server_url", serverUrl)
            .putString("premium_status", status)
            .putString("premium_expiry", expiry)
            .putString("premium_max_connections", maxConn)
            .putString("premium_active_connections", activeConn)
            .putString("premium_type", type)
            .putBoolean("premium_is_mock", isMock)
            .apply()

        _premiumUsername.value = username
        _premiumPassword.value = password
        _premiumServerUrl.value = serverUrl
        _premiumStatus.value = status
        _premiumExpiry.value = expiry
        _premiumMaxConnections.value = maxConn
        _premiumActiveConnections.value = activeConn
        _premiumType.value = type
        _premiumIsMock.value = isMock
    }

    fun setAvatarUrl(url: String) {
        prefs.edit().putString("avatar_url", url).apply()
        _avatarUrl.value = url
    }

    fun setAppTheme(theme: String) {
        prefs.edit().putString("app_theme", theme).apply()
        _appTheme.value = theme
    }

    fun setAutoplayNext(enabled: Boolean) {
        prefs.edit().putBoolean("autoplay_next", enabled).apply()
        _autoplayNext.value = enabled
    }

    fun setAutoFullscreen(enabled: Boolean) {
        prefs.edit().putBoolean("auto_fullscreen", enabled).apply()
        _autoFullscreen.value = enabled
    }

    fun setShowTmdb(enabled: Boolean) {
        prefs.edit().putBoolean("show_tmdb", enabled).apply()
        _showTmdb.value = enabled
    }

    fun generatePairingCode(): String {
        val code = (100000..999999).random().toString()
        _pairingCode.value = code
        return code
    }

    fun pairWithTv(enteredPin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val localIp = getLocalIpAddress()
            if (localIp == "127.0.0.1") {
                withContext(Dispatchers.Main) { onResult(false) }
                return@launch
            }
            val prefix = localIp.substringBeforeLast(".") + "."
            var matched = false
            
            // Scan subnet in parallel to find a listening TV on port 9999
            val jobs = (1..254).map { host ->
                launch {
                    val ip = prefix + host
                    try {
                        val socket = java.net.Socket()
                        socket.connect(java.net.InetSocketAddress(ip, 9999), 120) // Fast 120ms timeout
                        
                        val writer = java.io.PrintWriter(socket.getOutputStream(), true)
                        writer.println("PAIR_QUERY|$enteredPin|$localIp")
                        
                        val reader = java.io.BufferedReader(java.io.InputStreamReader(socket.getInputStream()))
                        val response = reader.readLine()
                        if (response != null && response.startsWith("PAIR_OK")) {
                            setTvIpAddress(ip)
                            _isTvConnected.value = true
                            matched = true
                            withContext(Dispatchers.Main) { onResult(true) }
                        }
                        socket.close()
                    } catch (e: Exception) {
                        // Skip
                    }
                }
            }
            jobs.forEach { it.join() }
            if (!matched) {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun disconnectFromTv() {
        _isTvConnected.value = false
        prefs.edit().putString("tv_ip_address", "").apply()
        _tvIpAddress.value = ""
    }

    fun clearPremiumProfile() {
        prefs.edit()
            .remove("premium_username")
            .remove("premium_password")
            .remove("premium_server_url")
            .remove("premium_status")
            .remove("premium_expiry")
            .remove("premium_max_connections")
            .remove("premium_active_connections")
            .remove("premium_created_at")
            .remove("premium_type")
            .remove("premium_is_mock")
            .apply()

        _premiumUsername.value = ""
        _premiumPassword.value = ""
        _premiumServerUrl.value = ""
        _premiumStatus.value = ""
        _premiumExpiry.value = ""
        _premiumMaxConnections.value = "1"
        _premiumActiveConnections.value = "0"
        _premiumCreatedAt.value = ""
        _premiumType.value = ""
        _premiumIsMock.value = false
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_selectedPlaylistId.value == playlistId) {
                val remaining = repository.getStaticAllPlaylists()
                selectPlaylist(remaining.firstOrNull()?.id)
            }
            if (_selectedChannel.value?.playlistId == playlistId) {
                _selectedChannel.value = null
                _activePlayUrl.value = null
                _isPlayingCatchup.value = false
                _selectedProgram.value = null
            }
        }
    }

    fun downloadActiveStream() {
        val channel = _selectedChannel.value ?: return
        val currentUrl = _activePlayUrl.value ?: return
        val label = if (_isPlayingCatchup.value && _selectedProgram.value != null) {
            "${channel.name} (Catchup: ${_selectedProgram.value?.title})"
        } else {
            "${channel.name} (Live)"
        }

        viewModelScope.launch {
            val downloadId = repository.createDownloadEntity(label, currentUrl)
            val path = File(getApplication<Application>().filesDir, "recording_${System.currentTimeMillis()}.ts").absolutePath
            repository.startDownload(downloadId, label, currentUrl, path)
        }
    }

    fun startDownload(downloadId: Int, channelName: String, streamUrl: String, filePath: String) {
        repository.startDownload(downloadId, channelName, streamUrl, filePath)
    }

    fun pauseDownload(id: Int) {
        repository.pauseDownload(id)
    }

    fun cancelDownload(id: Int) {
        repository.cancelDownload(id)
    }

    fun addCustomUrlDownload(name: String, url: String) {
        viewModelScope.launch {
            val downloadId = repository.createDownloadEntity(name, url)
            val path = File(getApplication<Application>().filesDir, "recording_${System.currentTimeMillis()}.ts").absolutePath
            repository.startDownload(downloadId, name, url, path)
        }
    }

    fun deleteDownload(download: DownloadEntity) {
        viewModelScope.launch {
            repository.deleteDownload(download.id, download.filePath)
        }
    }

    // --- TV & Mobile Controller Integration methods ---

    fun setDeviceMode(mode: String) {
        prefs.edit().putString("device_mode", mode).apply()
        _deviceMode.value = mode
        if (mode == "TV") {
            startRemoteServer()
        } else {
            stopRemoteServer()
        }
    }

    fun clearDeviceMode() {
        prefs.edit().remove("device_mode").apply()
        _deviceMode.value = null
        stopRemoteServer()
    }

    private var remoteServer: RemoteControlServer? = null

    fun startRemoteServer() {
        stopRemoteServer()
        val server = RemoteControlServer(
            pinSupplier = { _pairingCode.value },
            onCommandReceived = { command ->
                viewModelScope.launch {
                    handleRemoteCommand(command)
                }
            }
        )
        remoteServer = server
        server.start()
    }

    fun stopRemoteServer() {
        remoteServer?.stop()
        remoteServer = null
    }

    private suspend fun handleRemoteCommand(command: String) {
        try {
            val parts = command.trim().split("|")
            val action = parts[0]
            when (action) {
                "PAIR_QUERY" -> {
                    val phoneIp = parts.getOrNull(2) ?: ""
                    setTvIpAddress(phoneIp)
                    _isTvConnected.value = true
                }
                "UP" -> {
                    val channels = filteredChannels.value
                    val current = selectedChannel.value
                    if (channels.isNotEmpty()) {
                        val currentIndex = channels.indexOfFirst { it.id == current?.id }
                        val nextIndex = if (currentIndex <= 0) channels.size - 1 else currentIndex - 1
                        selectChannel(channels[nextIndex])
                    }
                }
                "DOWN" -> {
                    val channels = filteredChannels.value
                    val current = selectedChannel.value
                    if (channels.isNotEmpty()) {
                        val currentIndex = channels.indexOfFirst { it.id == current?.id }
                        val nextIndex = if (currentIndex == -1 || currentIndex >= channels.size - 1) 0 else currentIndex + 1
                        selectChannel(channels[nextIndex])
                    }
                }
                "PLAY_FEED" -> {
                    if (parts.size >= 3) {
                        val name = parts[1]
                        val streamUrl = parts[2]
                        val logoUrl = parts.getOrNull(3)?.takeIf { it.isNotEmpty() }
                        val groupTitle = parts.getOrNull(4)?.takeIf { it.isNotEmpty() } ?: "Shared Feed"
                        
                        val channel = ChannelEntity(
                            id = -100,
                            playlistId = -1,
                            name = name,
                            logoUrl = logoUrl,
                            groupTitle = groupTitle,
                            streamUrl = streamUrl,
                            tvgId = null,
                            tvgName = null,
                            catchupType = null,
                            catchupSource = null
                        )
                        selectChannel(channel)
                    }
                }
                "SEARCH" -> {
                    if (parts.size >= 2) {
                        setSearchQuery(parts[1])
                    }
                }
                "CATEGORY" -> {
                    if (parts.size >= 2) {
                        selectCategory(parts[1])
                    }
                }
                "STREAM_TYPE" -> {
                    if (parts.size >= 2) {
                        val typeStr = parts[1]
                        try {
                            selectStreamType(StreamType.valueOf(typeStr))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                "TOGGLE_FAVORITE" -> {
                    val current = selectedChannel.value
                    if (current != null) {
                        toggleFavorite(current)
                    }
                }
                "DOWNLOAD_ACTIVE" -> {
                    downloadActiveStream()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setTvIpAddress(ip: String) {
        prefs.edit().putString("tv_ip_address", ip).apply()
        _tvIpAddress.value = ip
    }

    fun connectToTv(ip: String, onResult: (Boolean) -> Unit) {
        setTvIpAddress(ip)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress(ip, 9999), 1500)
                socket.close()
                _isTvConnected.value = true
                withContext(Dispatchers.Main) { onResult(true) }
            } catch (e: Exception) {
                _isTvConnected.value = false
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun disconnectTv() {
        _isTvConnected.value = false
    }

    fun sendRemoteCommand(command: String) {
        val ip = _tvIpAddress.value
        if (ip.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress(ip, 9999), 1500)
                val writer = java.io.PrintWriter(socket.getOutputStream(), true)
                writer.println(command)
                socket.close()
                _isTvConnected.value = true
            } catch (e: Exception) {
                _isTvConnected.value = false
            }
        }
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                val addresses = java.util.Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress) {
                        val sAddr = address.hostAddress ?: ""
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (isIPv4 && sAddr.isNotEmpty()) return sAddr
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "127.0.0.1"
    }

    override fun onCleared() {
        super.onCleared()
        stopRemoteServer()
    }

    // Factory Class
    class Factory(
        private val application: Application,
        private val repository: IptvRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(IptvViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return IptvViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// Simple Server helper class for TV remote control commands
class RemoteControlServer(
    private val port: Int = 9999,
    private val pinSupplier: () -> String,
    private val onCommandReceived: (String) -> Unit
) {
    private var serverSocket: java.net.ServerSocket? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        Thread {
            try {
                serverSocket = java.net.ServerSocket(port)
                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    Thread {
                        handleClient(socket)
                    }.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverSocket = null
    }

    private fun handleClient(socket: java.net.Socket) {
        try {
            val reader = java.io.BufferedReader(java.io.InputStreamReader(socket.getInputStream()))
            val line = reader.readLine()
            if (line != null) {
                if (line.startsWith("PAIR_QUERY")) {
                    val parts = line.split("|")
                    val enteredPin = parts.getOrNull(1) ?: ""
                    val currentPin = pinSupplier()
                    val writer = java.io.PrintWriter(socket.getOutputStream(), true)
                    if (enteredPin == currentPin && currentPin.isNotEmpty()) {
                        writer.println("PAIR_OK")
                        onCommandReceived(line)
                    } else {
                        writer.println("PAIR_FAIL")
                    }
                } else {
                    onCommandReceived(line)
                }
            }
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
