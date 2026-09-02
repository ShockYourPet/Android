package app.shockyourpet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shockyourpet.data.api.models.CommandLogEntry
import app.shockyourpet.data.api.models.ShockerItem
import app.shockyourpet.data.api.models.ShockerPermissions
import app.shockyourpet.data.livecontrol.LiveControlClient
import app.shockyourpet.data.repository.OpenShockRepository
import app.shockyourpet.ui.components.InAppNotification
import app.shockyourpet.ui.components.NotificationType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShockerViewModel(val repository: OpenShockRepository) : ViewModel() {

    private val _shockers = MutableStateFlow<List<ShockerItem>>(emptyList())
    val shockers: StateFlow<List<ShockerItem>> = _shockers.asStateFlow()

    private val _selectedShocker = MutableStateFlow<ShockerItem?>(null)
    val selectedShocker: StateFlow<ShockerItem?> = _selectedShocker.asStateFlow()

    private val _intensity = MutableStateFlow(0)
    val intensity: StateFlow<Int> = _intensity.asStateFlow()

    private val _durationMs = MutableStateFlow(300)

    @Suppress("unused")
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val _activeMode = MutableStateFlow("Shock")

    @Suppress("unused")
    val activeMode: StateFlow<String> = _activeMode.asStateFlow()

    private val _isLoading = MutableStateFlow(value = false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _notification = MutableStateFlow<InAppNotification?>(null)
    val notification: StateFlow<InAppNotification?> = _notification.asStateFlow()

    val commandLogs: StateFlow<List<CommandLogEntry>> = repository.commandLogs

    private val liveControlClient: LiveControlClient = repository.createLiveControlClient(
        scope = viewModelScope,
    ) { actionType, statusText ->
        handleMissingPermission(actionType, statusText)
    }
    private var livePanelActive = false

    init {
        loadShockers()
    }

    override fun onCleared() {
        liveControlClient.disconnect()
        super.onCleared()
    }

    fun showNotification(message: String, type: NotificationType = NotificationType.Info, durationMs: Long = 3000L) {
        _notification.value = InAppNotification(message = message, type = type, durationMs = durationMs)
    }

    fun dismissNotification() {
        _notification.value = null
    }

    fun clearLogs() {
        repository.clearLogs()
        showNotification("Developer logs cleared", NotificationType.Info)
    }

    fun handleMissingPermission(actionType: String, statusText: String) {
        val current = _selectedShocker.value ?: return
        val currentPerms = current.permissions ?: ShockerPermissions()
        val activeModeLower = _activeMode.value.lowercase()

        val updatedPerms = when {
            activeModeLower.contains("sound") || statusText.lowercase().contains("sound") || actionType.lowercase().contains("sound") -> currentPerms.copy(allowSound = false)
            activeModeLower.contains("vibrate") || statusText.lowercase().contains("vibrate") || actionType.lowercase().contains("vibrate") -> currentPerms.copy(allowVibrate = false)
            activeModeLower.contains("shock") || statusText.lowercase().contains("shock") || actionType.lowercase().contains("shock") -> currentPerms.copy(allowShock = false)
            else -> currentPerms
        }

        val updatedShocker = current.copy(permissions = updatedPerms)
        _selectedShocker.value = updatedShocker
        _shockers.value = _shockers.value.map { if (it.id == updatedShocker.id) updatedShocker else it }

        viewModelScope.launch {
            showNotification("Permission missing for ${_activeMode.value} mode on ${current.name}", NotificationType.Warning)
        }
    }

    fun loadShockers() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getShockers()
            result.fold(
                onSuccess = { items ->
                    _shockers.value = items
                    val current = _selectedShocker.value
                    if (current != null) {
                        _selectedShocker.value = items.find { it.id == current.id } ?: current
                    } else if (items.isNotEmpty()) {
                        _selectedShocker.value = items.first()
                    }
                    if (livePanelActive) connectLiveControl()
                    _isLoading.value = false
                },
            ) { err ->
                _isLoading.value = false
                val msg = err.message ?: "Failed to load shockers"
                _userMessage.emit(msg)
                showNotification(msg, NotificationType.Error)
            }
        }
    }

    fun selectShocker(shocker: ShockerItem) {
        _selectedShocker.value = shocker
        showNotification("Selected ${shocker.name.ifBlank { shocker.id }}", NotificationType.Info)
        if (livePanelActive) connectLiveControl()
    }

    fun setActiveMode(type: String) {
        _activeMode.value = type
    }

    @Suppress("unused")
    fun setIntensity(value: Int) {
        _intensity.value = value.coerceIn(0, 100)
    }

    @Suppress("unused")
    fun setDuration(valueMs: Int) {
        _durationMs.value = valueMs.coerceIn(100, 10000)
    }

    fun onLivePanelActive() {
        if (repository.tokenManager.apiToken.isBlank()) {
            viewModelScope.launch {
                val msg = "Add an API token in Settings before using Live Panel."
                _userMessage.emit(msg)
                showNotification(msg, NotificationType.Warning)
            }
            return
        }
        livePanelActive = true
        connectLiveControl()
    }

    fun onLivePanelInactive() {
        livePanelActive = false
        stopLiveControl()
        liveControlClient.disconnect()
    }

    fun triggerThrottledLiveControl(type: String, targetIntensity: Int) {
        val cleanIntensity = targetIntensity.coerceIn(0, 100)
        _intensity.value = cleanIntensity
        liveControlClient.updateHolding(type, cleanIntensity)
    }

    fun stopLiveControl() {
        liveControlClient.stopHolding()
        _intensity.value = 0
    }

    fun addShockerById(shockerId: String) {
        if (shockerId.isNotBlank()) {
            val cleanId = shockerId.trim()
            repository.addCustomShockerId(cleanId)
            viewModelScope.launch {
                val msg = "Added shocker ID ${cleanId.take(8)}"
                _userMessage.emit(msg)
                showNotification(msg, NotificationType.Success)
                loadShockers()
            }
        }
    }

    fun removeShockerById(shockerId: String) {
        repository.removeCustomShockerId(shockerId)
        showNotification("Removed shocker ${shockerId.take(8)}", NotificationType.Info)
        loadShockers()
    }

    private fun connectLiveControl() {
        val shocker = _selectedShocker.value ?: return
        val hubId = shocker.device?.id?.ifBlank { shocker.id } ?: shocker.id
        if (hubId.isBlank()) {
            viewModelScope.launch {
                val msg = "No hub id for ${shocker.name}. Pull to refresh Shockers."
                _userMessage.emit(msg)
                showNotification(msg, NotificationType.Warning)
            }
            return
        }
        val name = shocker.name.ifBlank { "Shocker ${shocker.id.take(8)}" }
        liveControlClient.connect(hubId, shocker.id, name)
    }
}
