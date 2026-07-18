import sys
import re

def main():
    with open("app/src/main/java/com/example/viewmodel/IptvViewModel.kt", "r") as f:
        content = f.read()

    new_prefs = """
    private val _dolbyAudio = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("dolby_audio", false))
    val dolbyAudio: kotlinx.coroutines.flow.StateFlow<Boolean> = kotlinx.coroutines.flow.asStateFlow(_dolbyAudio)

    private val _enableCc = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("enable_cc", false))
    val enableCc: kotlinx.coroutines.flow.StateFlow<Boolean> = kotlinx.coroutines.flow.asStateFlow(_enableCc)

    private val _useVlcPlayer = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("use_vlc_player", false))
    val useVlcPlayer: kotlinx.coroutines.flow.StateFlow<Boolean> = kotlinx.coroutines.flow.asStateFlow(_useVlcPlayer)
    """

    content = re.sub(r'(val showTmdb: StateFlow<Boolean> = _showTmdb\.asStateFlow\(\))', r'\1\n' + new_prefs, content)

    new_setters = """
    fun setDolbyAudio(enabled: Boolean) {
        prefs.edit().putBoolean("dolby_audio", enabled).apply()
        _dolbyAudio.value = enabled
    }

    fun setEnableCc(enabled: Boolean) {
        prefs.edit().putBoolean("enable_cc", enabled).apply()
        _enableCc.value = enabled
    }

    fun setUseVlcPlayer(useVlc: Boolean) {
        prefs.edit().putBoolean("use_vlc_player", useVlc).apply()
        _useVlcPlayer.value = useVlc
    }
    """

    content = re.sub(r'(fun setShowTmdb\(show: Boolean\) \{[\s\S]*?\})', r'\1\n' + new_setters, content)

    with open("app/src/main/java/com/example/viewmodel/IptvViewModel.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
