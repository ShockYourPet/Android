package app.shockyourpet.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.shockyourpet.ui.theme.AppThemeStyle
import app.shockyourpet.ui.viewmodel.ConnectionState
import app.shockyourpet.ui.viewmodel.SettingsViewModel
import app.shockyourpet.ui.viewmodel.ShockerViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    shockerViewModel: ShockerViewModel,
    themeStyle: AppThemeStyle,
    onOpenAppearance: () -> Unit,
    onOpenDevice: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenShared: () -> Unit,
    onOpenHost: () -> Unit,
    onOpenDeveloper: () -> Unit,
) {
    val connectionState by settingsViewModel.connectionState.collectAsState()
    val sharedKeys by settingsViewModel.sharedApiKeys.collectAsState()
    val useCustomServer by settingsViewModel.useCustomServer.collectAsState()
    val serverUrl by settingsViewModel.serverUrl.collectAsState()
    val shockers by shockerViewModel.shockers.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Appearance, connections, and account preferences",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(22.dp))

        SettingsGroup {
            SettingsNavigationRow(Icons.Default.Palette, "Appearance", themeStyle.displayName, onOpenAppearance)
            SettingsDivider()
            SettingsNavigationRow(Icons.Default.Add, "Add device", "${shockers.size} devices connected", onOpenDevice)
        }

        SettingsGroupLabel("CONNECTIONS")
        SettingsGroup {
            SettingsNavigationRow(
                Icons.Default.Person,
                "Account & sign-in",
                if (connectionState is ConnectionState.Success) "Connected" else "Not connected",
                onOpenAccount,
            )
            SettingsDivider()
            SettingsNavigationRow(Icons.Default.Share, "Shared access", "${sharedKeys.size} saved keys", onOpenShared)
            SettingsDivider()
            SettingsNavigationRow(
                Icons.Default.Public,
                "Configured host",
                if (useCustomServer) serverUrl else "OpenShock default",
                onOpenHost,
            )
        }

        SettingsGroupLabel("ADVANCED")
        SettingsGroup {
            SettingsNavigationRow(Icons.Default.Terminal, "Developer options", "Logs and diagnostics", onOpenDeveloper)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun AppearanceSettingsScreen(
    selectedTheme: AppThemeStyle,
    onThemeSelected: (AppThemeStyle) -> Unit,
    onBack: () -> Unit,
) {
    SettingsDetailPage("Appearance", onBack) {
        Text("Choose a palette", style = MaterialTheme.typography.titleLarge)
        Text(
            "All four options keep the same soft preppy styling.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        ThemeOptionRow(AppThemeStyle.Blush, "Bright pink and cream", selectedTheme, onThemeSelected)
        Spacer(Modifier.height(10.dp))
        ThemeOptionRow(AppThemeStyle.RoseTea, "Muted rose and warm taupe", selectedTheme, onThemeSelected)
        Spacer(Modifier.height(10.dp))
        ThemeOptionRow(AppThemeStyle.RoseDusk, "A softer, lighter dark palette", selectedTheme, onThemeSelected)
        Spacer(Modifier.height(10.dp))
        ThemeOptionRow(AppThemeStyle.BerryNight, "Deep berry and charcoal plum", selectedTheme, onThemeSelected)
    }
}

@Composable
fun DeviceSetupScreen(viewModel: ShockerViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val shockers by viewModel.shockers.collectAsState()
    var deviceId by remember { mutableStateOf("") }
    SettingsDetailPage("Add device", onBack) {
        Text("Connect a device", style = MaterialTheme.typography.titleLarge)
        Text(
            "Enter a device ID or share code. The device will appear in your list after it connects.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = deviceId,
            onValueChange = { deviceId = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Device ID or share code") },
            leadingIcon = { Icon(Icons.Default.Pets, contentDescription = null) },
            singleLine = true,
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                viewModel.addShockerById(deviceId)
                deviceId = ""
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = deviceId.isNotBlank(),
        ) {
            Text("Connect device")
        }

        Spacer(Modifier.height(28.dp))
        SettingsSectionLabel("CONNECTED DEVICES")
        if (shockers.isEmpty()) {
            Text(
                "No connected devices yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            shockers.forEach { shocker ->
                val ownerName = shocker.owner?.username ?: if (shocker.isShared) "Shared" else "Owned"
                val hubName = shocker.device?.name?.takeIf { it.isNotBlank() }
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                shocker.name.ifBlank { "Connected device" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                buildString {
                                    append("Owner: $ownerName")
                                    hubName?.let { append(" • Hub: $it") }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "ID: ${shocker.id}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Shocker ID", shocker.id))
                                viewModel.showNotification("Copied shocker ID", durationMs = 2000L)
                            },
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID")
                        }
                        IconButton(
                            onClick = { viewModel.removeShockerById(shocker.id) },
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove device", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val apiToken by viewModel.apiToken.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val accountProfile by viewModel.accountProfile.collectAsState()
    val useCustomServer by viewModel.useCustomServer.collectAsState()

    var tokenDraft by remember(apiToken) { mutableStateOf(apiToken) }
    var showToken by remember { mutableStateOf(value = false) }

    SettingsDetailPage("Account & sign-in", onBack) {
        Text("Account status", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        ConnectionStatus(
            state = connectionState,
            serverUrl = serverUrl,
        ) {
            viewModel.updateApiToken("")
            tokenDraft = ""
        }

        Spacer(Modifier.height(24.dp))
        SettingsSectionLabel("AUTOMATIC SIGN-IN")
        Text(
            if (connectionState is ConnectionState.Success) {
                "Generate a fresh token for this app with the full required permissions."
            } else {
                "Sign in on OpenShock, review the requested permission, and the new token will return to this app automatically."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val callback = "shockyourpet://auth/callback?token=%"
                val requestUri = "https://next.openshock.app/settings/api-tokens/new"
                    .toUri()
                    .buildUpon()
                    .appendQueryParameter("name", "Shock Your Pet")
                    .appendQueryParameter(
                        "permissions",
                        listOf(
                            "shockers.use",
                            "shockers.edit",
                            "shockers.pause",
                            "devices.edit",
                            "devices.auth",
                            "usershares.edit",
                            "usershares.pause",
                            "publicshares.edit",
                            "publicshares.pause",
                        ).joinToString(","),
                    )
                    .appendQueryParameter("redirect_uri", callback)
                    .build()
                context.startActivity(Intent(Intent.ACTION_VIEW, requestUri))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (connectionState is ConnectionState.Success) "Create replacement token" else "Continue in OpenShock")
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(17.dp))
        }
        if (useCustomServer) {
            Spacer(Modifier.height(8.dp))
            Text(
                "The automatic flow uses the official OpenShock site. For a custom host, paste its token below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
        SettingsSectionLabel("SAVED API TOKEN")
        Text(
            "Stored on this device and kept available after you close the app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = tokenDraft,
            onValueChange = { tokenDraft = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Account API token") },
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
            trailingIcon = {
                Row {
                    if (apiToken.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("OpenShock API token", apiToken))
                            },
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy saved token")
                        }
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val value = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim().orEmpty()
                            if (value.isNotBlank()) tokenDraft = value
                        },
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste token")
                    }
                    IconButton(onClick = { showToken = !showToken }) {
                        Icon(if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Show token")
                    }
                }
            },
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
        )
        accountProfile?.let { profile ->
            if (profile.tokenName.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Current token: ${profile.tokenName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (profile.permissions.isNotEmpty()) {
                Text(
                    "Permissions: ${profile.permissions.joinToString(", ") { permissionLabel(it) }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                viewModel.updateApiToken(tokenDraft)
                viewModel.validateConnection()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = tokenDraft.isNotBlank() && (connectionState !is ConnectionState.Validating),
        ) {
            if (connectionState is ConnectionState.Validating) {
                CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (apiToken.isNotBlank()) "Save and validate token" else "Connect with token")
        }
    }
}

@Composable
fun SharedAccessSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val keys by viewModel.sharedApiKeys.collectAsState()
    val activeToken by viewModel.apiToken.collectAsState()
    var label by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }

    SettingsDetailPage("Shared access", onBack) {
        Text("Saved access", style = MaterialTheme.typography.titleLarge)
        Text(
            "Keys shared with you for partner hubs or devices.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        if (keys.isEmpty()) {
            Text("No shared keys saved.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            keys.forEach { key ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (key.token == activeToken) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(key.name, style = MaterialTheme.typography.titleMedium)
                            Text(if (key.token == activeToken) "Active" else "${key.token.take(8)}…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (key.token != activeToken) {
                            TextButton(onClick = { viewModel.selectSharedApiKey(key) }) { Text("Use") }
                        }
                        IconButton(onClick = { viewModel.removeSharedApiKey(key.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SettingsSectionLabel("ADD SHARED ACCESS")
        OutlinedTextField(label, { label = it }, Modifier.fillMaxWidth(), label = { Text("Label") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            token,
            { token = it },
            Modifier.fillMaxWidth(),
            label = { Text("Shared API key") },
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                viewModel.addSharedApiKey(label, token)
                label = ""
                token = ""
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = token.isNotBlank(),
        ) { Text("Save shared access") }
    }
}

@Composable
fun HostSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val useCustomServer by viewModel.useCustomServer.collectAsState()
    SettingsDetailPage("Configured host", onBack) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Use a custom server", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (useCustomServer) "Custom host enabled" else "Using https://api.openshock.app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(useCustomServer, viewModel::toggleCustomServer)
        }
        if (useCustomServer) {
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                serverUrl,
                viewModel::updateServerUrl,
                Modifier.fillMaxWidth(),
                label = { Text("Server base URL") },
                leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) },
                singleLine = true,
            )
        }
    }
}

@Composable
fun DeveloperSettingsScreen(
    settingsViewModel: SettingsViewModel,
    shockerViewModel: ShockerViewModel,
    onBack: () -> Unit,
) {
    var showTerminal by remember { mutableStateOf(value = false) }
    val developerDebugLogging by settingsViewModel.developerDebugLogging.collectAsState()

    SettingsDetailPage("Developer options", onBack) {
        Text("Diagnostics", style = MaterialTheme.typography.titleLarge)
        Text(
            "Inspect live WebSocket frames, API requests, and command events.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Developer Debug Logging", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (developerDebugLogging) "Logging all debug & connection events" else "Logging Errors & Warnings only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = developerDebugLogging,
                    onCheckedChange = { settingsViewModel.toggleDeveloperDebugLogging(it) },
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Button(onClick = { showTerminal = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Terminal, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Open system logs")
        }
    }
    if (showTerminal) {
        TerminalLogsDialog(shockerViewModel, settingsViewModel) { showTerminal = false }
    }
}

@Composable
private fun SettingsDetailPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
            content = content,
        )
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        content = { Column(content = content) },
    )
}

@Composable
private fun SettingsNavigationRow(icon: ImageVector, title: String, summary: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softPressClickable(MaterialTheme.shapes.small, onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(38.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(Modifier.padding(start = 66.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SettingsGroupLabel(text: String) {
    Spacer(Modifier.height(22.dp))
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(8.dp))
}

private fun permissionLabel(value: String): String = when (value.lowercase()) {
    "shockers.use" -> "use shockers"
    "shockers.edit" -> "edit shockers"
    "shockers.pause" -> "pause shockers"
    "devices.edit" -> "edit hubs"
    "devices.auth" -> "pair hubs"
    "usershares.edit" -> "edit user shares"
    "usershares.pause" -> "pause user shares"
    "publicshares.edit" -> "edit public shares"
    "publicshares.pause" -> "pause public shares"
    else -> value
}

@Composable
private fun ThemeOptionRow(
    style: AppThemeStyle,
    description: String,
    selected: AppThemeStyle,
    onSelected: (AppThemeStyle) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (style == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (style == selected) 2.dp else 1.dp, if (style == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .softPressClickable(MaterialTheme.shapes.medium) { onSelected(style) }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = style == selected, onClick = { onSelected(style) })
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(style.displayName, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    if (style.isDark) Color(0xFF2D2028) else Color(0xFFFFE2EC),
                    if ((style == AppThemeStyle.RoseTea) || (style == AppThemeStyle.RoseDusk)) Color(0xFF9C747F) else Color(0xFFE85F96),
                    if (style.isDark) Color(0xFFF2A9C1) else Color(0xFFFFF7F4),
                ).forEach { swatch -> Surface(Modifier.size(16.dp), CircleShape, swatch) {} }
            }
        }
    }
}

@Composable
private fun Modifier.softPressClickable(shape: Shape, onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    return this
        .clip(shape)
        .background(
            if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else Color.Transparent,
        )
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
}

@Composable
private fun ConnectionStatus(state: ConnectionState, serverUrl: String, onLogout: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = when (state) {
            is ConnectionState.Success -> MaterialTheme.colorScheme.tertiaryContainer
            is ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (state) {
                    is ConnectionState.Success -> Icons.Default.CheckCircle
                    is ConnectionState.Error -> Icons.Default.Error
                    else -> Icons.Default.Person
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when (state) {
                        is ConnectionState.Success -> state.username
                        is ConnectionState.Error -> "Connection needs attention"
                        is ConnectionState.Validating -> "Checking connection…"
                        else -> "Not connected"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    (state as? ConnectionState.Error)?.message ?: serverUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state is ConnectionState.Success) {
                IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out") }
            }
        }
    }
}
