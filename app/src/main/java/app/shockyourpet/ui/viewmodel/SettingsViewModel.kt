package app.shockyourpet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shockyourpet.data.api.OpenShockAuth
import app.shockyourpet.data.api.models.SharedApiKey
import app.shockyourpet.data.api.models.UserData
import app.shockyourpet.data.repository.OpenShockRepository
import app.shockyourpet.ui.components.InAppNotification
import app.shockyourpet.ui.components.NotificationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Validating : ConnectionState
    data class Success(val username: String) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

class SettingsViewModel(private val repository: OpenShockRepository) : ViewModel() {

    val tokenManager = repository.tokenManager

    private val _apiToken = MutableStateFlow(tokenManager.apiToken)
    val apiToken: StateFlow<String> = _apiToken.asStateFlow()

    private val _serverUrl = MutableStateFlow(tokenManager.serverUrl)
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _useCustomServer = MutableStateFlow(tokenManager.useCustomServer)
    val useCustomServer: StateFlow<Boolean> = _useCustomServer.asStateFlow()

    private val _developerDebugLogging = MutableStateFlow(tokenManager.developerDebugLogging)
    val developerDebugLogging: StateFlow<Boolean> = _developerDebugLogging.asStateFlow()

    private val _sharedApiKeys = MutableStateFlow(tokenManager.getSharedApiKeys())
    val sharedApiKeys: StateFlow<List<SharedApiKey>> = _sharedApiKeys.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _accountProfile = MutableStateFlow<UserData?>(null)
    val accountProfile: StateFlow<UserData?> = _accountProfile.asStateFlow()

    private val _notification = MutableStateFlow<InAppNotification?>(null)
    val notification: StateFlow<InAppNotification?> = _notification.asStateFlow()

    init {
        if (tokenManager.apiToken.isNotBlank()) {
            validateConnection()
        }
    }

    fun showNotification(message: String, type: NotificationType = NotificationType.Info, durationMs: Long = 3000L) {
        _notification.value = InAppNotification(message = message, type = type, durationMs = durationMs)
    }

    fun dismissNotification() {
        _notification.value = null
    }

    fun updateApiToken(token: String) {
        val cleaned = OpenShockAuth.sanitizeToken(token)
        _apiToken.value = cleaned
        tokenManager.apiToken = cleaned
        repository.invalidateApiClient()
        showNotification("Updated OpenShock Account Token", NotificationType.Info)
    }

    fun acceptApiTokenCallback(token: String) {
        val cleaned = OpenShockAuth.sanitizeToken(token)
        val formatError = OpenShockAuth.validateTokenFormat(cleaned)
        if (formatError != null) {
            _connectionState.value = ConnectionState.Error(formatError)
            showNotification("OpenShock returned an invalid token", NotificationType.Error)
            return
        }
        updateApiToken(cleaned)
        showNotification("Token saved on this device", NotificationType.Success)
        validateConnection()
    }

    fun updateServerUrl(url: String) {
        _serverUrl.value = url
        tokenManager.serverUrl = url
        repository.invalidateApiClient()
        showNotification("Updated Server Base URL", NotificationType.Info)
    }

    fun toggleCustomServer(enabled: Boolean) {
        _useCustomServer.value = enabled
        tokenManager.useCustomServer = enabled
        repository.invalidateApiClient()
        val serverName = if (enabled) "Custom Server Enabled" else "Using Default Server"
        showNotification(serverName, NotificationType.Info)
    }

    fun toggleDeveloperDebugLogging(enabled: Boolean) {
        _developerDebugLogging.value = enabled
        tokenManager.developerDebugLogging = enabled
        val msg = if (enabled) "Developer Debug Logging enabled" else "System logs will record Errors & Warnings only"
        showNotification(msg, NotificationType.Info)
    }

    fun addSharedApiKey(name: String, token: String) {
        val key = tokenManager.addSharedApiKey(name, token)
        _sharedApiKeys.value = tokenManager.getSharedApiKeys()
        showNotification("Added shared key '${key.name}'", NotificationType.Success)
    }

    fun removeSharedApiKey(id: String) {
        tokenManager.removeSharedApiKey(id)
        _sharedApiKeys.value = tokenManager.getSharedApiKeys()
        showNotification("Removed shared key", NotificationType.Info)
    }

    fun selectSharedApiKey(key: SharedApiKey) {
        updateApiToken(key.token)
        updateServerUrl(key.serverUrl)
        showNotification("Switched active key to '${key.name}'", NotificationType.Success)
        validateConnection()
    }

    fun validateConnection() {
        if (_apiToken.value.isBlank()) {
            _accountProfile.value = null
            _connectionState.value = ConnectionState.Error("API Token is required")
            showNotification("API Token is required", NotificationType.Warning)
            return
        }

        viewModelScope.launch {
            _connectionState.value = ConnectionState.Validating
            val result = repository.validateToken()
            result.fold(
                onSuccess = { user ->
                    val name = user.username.ifBlank { "Connected User" }
                    _accountProfile.value = user
                    _connectionState.value = ConnectionState.Success(name)
                    tokenManager.updateActiveConnectionName(name)
                    showNotification("Connected as $name", NotificationType.Success)
                },
            ) { err ->
                val msg = err.message ?: "Connection failed"
                _accountProfile.value = null
                _connectionState.value = ConnectionState.Error(msg)
                showNotification(msg, NotificationType.Error)
            }
        }
    }

    @Suppress("unused")
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Validating
            val result = repository.login(email, pass)
            result.fold(
                onSuccess = { token ->
                    _apiToken.value = token
                    showNotification("Logged in to OpenShock Account successfully", NotificationType.Success)
                    validateConnection()
                },
            ) { err ->
                val msg = err.message ?: "Login failed"
                _connectionState.value = ConnectionState.Error(msg)
                showNotification(msg, NotificationType.Error)
            }
        }
    }

    @Suppress("unused")
    fun logout() {
        tokenManager.apiToken = ""
        tokenManager.primaryAccountUsername = ""
        _apiToken.value = ""
        _accountProfile.value = null
        _connectionState.value = ConnectionState.Idle
        showNotification("Disconnected account", NotificationType.Info)
    }
}
