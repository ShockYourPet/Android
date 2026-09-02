package app.shockyourpet.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavRoute(val route: String, val label: String, val icon: ImageVector) {
    data object Shockers : NavRoute("shockers", "Shockers", Icons.Default.Pets)
    data object LivePanel : NavRoute("live", "Live Panel", Icons.AutoMirrored.Filled.VolumeUp)
    data object Settings : NavRoute("settings", "Settings", Icons.Default.Settings)
    data object SettingsAppearance : NavRoute("settings/appearance", "Appearance", Icons.Default.Palette)
    data object SettingsDevice : NavRoute("settings/device", "Add Device", Icons.Default.Add)
    data object SettingsAccount : NavRoute("settings/account", "Account", Icons.Default.Person)
    data object SettingsShared : NavRoute("settings/shared", "Shared Access", Icons.Default.Share)
    data object SettingsHost : NavRoute("settings/host", "Configured Host", Icons.Default.Public)
    data object SettingsDeveloper : NavRoute("settings/developer", "Developer Options", Icons.Default.Terminal)
}
