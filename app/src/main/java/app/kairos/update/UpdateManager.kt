package app.kairos.update

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val update: AvailableUpdate) : UpdateState
    data class Downloading(val update: AvailableUpdate, val percent: Int) : UpdateState
    data class Failed(val message: String, val update: AvailableUpdate? = null) : UpdateState
}

/** Güncelleme durumu: Bugün ekranındaki şerit ve Ayarlar > Güncellemeler bunu izler. */
class UpdateManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repo: String,
    private val currentVersion: String,
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    fun check() {
        if (_state.value is UpdateState.Checking || _state.value is UpdateState.Downloading) return
        _state.value = UpdateState.Checking
        scope.launch {
            _state.value = runCatching { UpdateChecker.check(repo, currentVersion) }.fold(
                onSuccess = { it?.let(UpdateState::Available) ?: UpdateState.UpToDate },
                onFailure = {
                    Log.w("UpdateManager", "Güncelleme denetlenemedi", it)
                    UpdateState.Failed("Denetlenemedi. İnternet bağlantınızı kontrol edin.")
                },
            )
        }
    }

    /** İndirir ve kurulumu başlatır. Yükleme izni çağıran tarafından önceden denetlenir. */
    fun downloadAndInstall(update: AvailableUpdate) {
        if (_state.value is UpdateState.Downloading) return
        _state.value = UpdateState.Downloading(update, 0)
        scope.launch {
            try {
                val apk = UpdateInstaller.download(context, update) { p -> _state.value = UpdateState.Downloading(update, p) }
                UpdateInstaller.install(context, apk)
                _state.value = UpdateState.Available(update) // onay ekranı kapatılırsa yeniden denenebilsin
            } catch (e: Exception) {
                Log.w("UpdateManager", "Güncelleme indirilemedi", e)
                _state.value = UpdateState.Failed("İndirilemedi. Tekrar deneyin.", update)
            }
        }
    }
}
