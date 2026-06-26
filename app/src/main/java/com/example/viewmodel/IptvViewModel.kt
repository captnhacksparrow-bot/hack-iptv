package com.example.viewmodel

import android.app.Application
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

    // Playlists UI State
    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // Dynamic combined list of Categories (Favorites, All, + parsed groups) filtered by StreamType
    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<String>> = combine(
        repository.allChannels,
        _selectedStreamType
    ) { allChans, streamType ->
        val groups = allChans
            .filter { it.getStreamType() == streamType }
            .map { it.groupTitle }
            .distinct()
            .filter { it.isNotEmpty() }
            .sorted()
        listOf("All", "Favorites") + groups
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All", "Favorites"))

    // Channel Filtering State based on category, query, and streamType
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredChannels: StateFlow<List<ChannelEntity>> = combine(
        _selectedCategory,
        _searchQuery,
        _selectedStreamType
    ) { category, query, streamType ->
        Triple(category, query, streamType)
    }.flatMapLatest { (category, query, streamType) ->
        repository.getChannelsByCategory(category).map { channels ->
            val typedChannels = channels.filter { it.getStreamType() == streamType }
            if (query.isEmpty()) {
                typedChannels
            } else {
                typedChannels.filter { it.name.contains(query, ignoreCase = true) }
            }
        }
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

    init {
        // Auto-load default Capt'n Hack Premium M3U playlists if none exist
        viewModelScope.launch {
            try {
                // 1. Generate local premium VOD and PPV playlist file
                val localM3uFile = File(getApplication<Application>().filesDir, "premium_vod_ppv.m3u")
                if (!localM3uFile.exists()) {
                    val m3uContent = """
                        #EXTM3U
                        #EXTINF:-1 tvg-id="movie-sintel" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/8/8f/Sintel_poster.jpg" group-title="Movies (On Demand)",Sintel (On Demand Movie)
                        https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4
                        #EXTINF:-1 tvg-id="movie-tears" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/0/01/Tears_of_Steel_poster.jpg" group-title="Movies (On Demand)",Tears of Steel (VOD Sci-Fi)
                        https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4
                        #EXTINF:-1 tvg-id="movie-bunny" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/c/c5/Big_Buck_Bunny_Main_Poster.jpg" group-title="Movies (On Demand)",Big Buck Bunny (On Demand)
                        https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4
                        #EXTINF:-1 tvg-id="movie-elephants" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/e/e8/Elephants_Dream_poster.jpg" group-title="Movies (On Demand)",Elephants Dream (VOD Classic)
                        https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4
                        #EXTINF:-1 tvg-id="show-sintel-ep1" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/8/8f/Sintel_poster.jpg" group-title="TV Shows (On Demand)",Sintel Season 1 Episode 1
                        https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4
                        #EXTINF:-1 tvg-id="show-tears-ep1" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/0/01/Tears_of_Steel_poster.jpg" group-title="TV Shows (On Demand)",Tears of Steel - Pilot Episode 1
                        https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4
                        #EXTINF:-1 tvg-id="show-bunny-ep1" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/c/c5/Big_Buck_Bunny_Main_Poster.jpg" group-title="TV Shows (On Demand)",Big Buck Bunny - Pilot Episode
                        https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4
                        #EXTINF:-1 tvg-id="ppv-ufc" tvg-logo="https://images.unsplash.com/photo-1517649763962-0c623066013b?w=200" group-title="PPV Events",PPV UFC Live: Heavyweight Championship
                        https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutofBounds.mp4
                        #EXTINF:-1 tvg-id="ppv-boxing" tvg-logo="https://images.unsplash.com/photo-1517649763962-0c623066013b?w=200" group-title="PPV Events",PPV Boxing: World Title Fight Night
                        https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4
                        #EXTINF:-1 tvg-id="ppv-wwe" tvg-logo="https://images.unsplash.com/photo-1517649763962-0c623066013b?w=200" group-title="PPV Events",PPV WWE WrestleMania Showcase
                        https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4
                    """.trimIndent()
                    localM3uFile.writeText(m3uContent)
                }

                val currentPlaylists = repository.playlists.first()
                if (currentPlaylists.isEmpty()) {
                    // Prepopulate both
                    addPlaylist(
                        name = "Capt'n Hack Premium VOD & PPV",
                        url = "file://${localM3uFile.absolutePath}"
                    )
                    addPlaylist(
                        name = "Capt'n Hack Premium News",
                        url = "https://iptv-org.github.io/iptv/categories/news.m3u"
                    )
                } else {
                    if (currentPlaylists.none { it.name == "Capt'n Hack Premium VOD & PPV" }) {
                        addPlaylist(
                            name = "Capt'n Hack Premium VOD & PPV",
                            url = "file://${localM3uFile.absolutePath}"
                        )
                    }
                    if (currentPlaylists.none { it.name == "Capt'n Hack Premium News" }) {
                        addPlaylist(
                            name = "Capt'n Hack Premium News",
                            url = "https://iptv-org.github.io/iptv/categories/news.m3u"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                val currentPlaylists = repository.playlists.first()
                for (playlist in currentPlaylists) {
                    repository.refreshPlaylist(playlist.id)
                }
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
            if (!success) {
                _playlistAddError.value = "Failed to parse playlist. Check URL."
            }
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
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
            _downloadProgresses.update { it + (currentUrl to 0f) }
            repository.downloadStream(label, currentUrl) { progress, _ ->
                _downloadProgresses.update { it + (currentUrl to progress) }
            }
            _downloadProgresses.update { it - currentUrl }
        }
    }

    fun deleteDownload(download: DownloadEntity) {
        viewModelScope.launch {
            repository.deleteDownload(download.id, download.filePath)
        }
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
