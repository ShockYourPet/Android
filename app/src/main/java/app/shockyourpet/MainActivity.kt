package app.shockyourpet

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.shockyourpet.data.api.OpenShockApiClient
import app.shockyourpet.data.repository.OpenShockRepository
import app.shockyourpet.data.storage.TokenManager
import app.shockyourpet.ui.components.InAppNotificationBanner
import app.shockyourpet.ui.components.PreppyBackground
import app.shockyourpet.ui.navigation.NavRoute
import app.shockyourpet.ui.screens.AccountSettingsScreen
import app.shockyourpet.ui.screens.AppearanceSettingsScreen
import app.shockyourpet.ui.screens.DeveloperSettingsScreen
import app.shockyourpet.ui.screens.DeviceSetupScreen
import app.shockyourpet.ui.screens.HostSettingsScreen
import app.shockyourpet.ui.screens.LivePanelScreen
import app.shockyourpet.ui.screens.SettingsScreen
import app.shockyourpet.ui.screens.SharedAccessSettingsScreen
import app.shockyourpet.ui.screens.ShockerListScreen
import app.shockyourpet.ui.theme.AppThemeStyle
import app.shockyourpet.ui.theme.ShockYourPetTheme
import app.shockyourpet.ui.viewmodel.SettingsViewModel
import app.shockyourpet.ui.viewmodel.ShockerViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var apiClient: OpenShockApiClient
    private lateinit var repository: OpenShockRepository
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var shockerViewModel: ShockerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager = TokenManager(this)
        apiClient = OpenShockApiClient(tokenManager)
        repository = OpenShockRepository(tokenManager, apiClient)
        settingsViewModel = SettingsViewModel(repository)
        shockerViewModel = ShockerViewModel(repository)
        handleTokenCallback(intent)

        val appearancePreferences = getSharedPreferences("appearance_preferences", MODE_PRIVATE)

        setContent {
            var themeStyle by rememberSaveable {
                mutableStateOf(AppThemeStyle.fromStorageKey(appearancePreferences.getString("theme_style", null)))
            }

            SideEffect {
                val systemBars = if (themeStyle.isDark) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = systemBars, navigationBarStyle = systemBars)
            }

            ShockYourPetTheme(style = themeStyle) {
                OpenShockAppScaffold(
                    settingsViewModel = settingsViewModel,
                    shockerViewModel = shockerViewModel,
                    themeStyle = themeStyle,
                ) { selected ->
                    themeStyle = selected
                    appearancePreferences.edit { putString("theme_style", selected.storageKey) }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTokenCallback(intent)
    }

    private fun handleTokenCallback(intent: Intent?) {
        val callback = intent?.data ?: return
        if ((callback.scheme != "shockyourpet") || (callback.host != "auth") || (callback.path != "/callback")) return
        callback.getQueryParameter("token")
            ?.takeIf { it.isNotBlank() }
            ?.let(settingsViewModel::acceptApiTokenCallback)
    }
}

@Composable
fun OpenShockAppScaffold(
    settingsViewModel: SettingsViewModel,
    shockerViewModel: ShockerViewModel,
    themeStyle: AppThemeStyle,
    onThemeStyleChange: (AppThemeStyle) -> Unit,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val shockerNotification by shockerViewModel.notification.collectAsState()
    val settingsNotification by settingsViewModel.notification.collectAsState()
    val activeNotification = shockerNotification ?: settingsNotification
    val navItems = listOf(NavRoute.Shockers, NavRoute.LivePanel, NavRoute.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNavigation = navItems.any { it.route == currentRoute }

    LaunchedEffect(Unit) {
        shockerViewModel.userMessage.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    PreppyBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomNavigation) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                    ) {
                        navItems.forEach { item ->
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                selected = currentRoute == item.route,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(NavRoute.Shockers.route) { saveState = false }
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
                NavHost(
                    navController = navController,
                    startDestination = NavRoute.Shockers.route,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable(NavRoute.Shockers.route) {
                        ShockerListScreen(shockerViewModel) {
                            navController.navigate(NavRoute.LivePanel.route) { launchSingleTop = true }
                        }
                    }
                    composable(NavRoute.LivePanel.route) {
                        LivePanelScreen(shockerViewModel) {
                            if (!navController.popBackStack()) {
                                navController.navigate(NavRoute.Shockers.route) { launchSingleTop = true }
                            }
                        }
                    }
                    composable(NavRoute.Settings.route) {
                        SettingsScreen(
                            settingsViewModel = settingsViewModel,
                            shockerViewModel = shockerViewModel,
                            themeStyle = themeStyle,
                            onOpenAppearance = { navController.navigate(NavRoute.SettingsAppearance.route) },
                            onOpenDevice = { navController.navigate(NavRoute.SettingsDevice.route) },
                            onOpenAccount = { navController.navigate(NavRoute.SettingsAccount.route) },
                            onOpenShared = { navController.navigate(NavRoute.SettingsShared.route) },
                            onOpenHost = { navController.navigate(NavRoute.SettingsHost.route) },
                        ) { navController.navigate(NavRoute.SettingsDeveloper.route) }
                    }
                    composable(NavRoute.SettingsAppearance.route) {
                        AppearanceSettingsScreen(themeStyle, onThemeStyleChange) { navController.popBackStack() }
                    }
                    composable(NavRoute.SettingsDevice.route) {
                        DeviceSetupScreen(shockerViewModel) { navController.popBackStack() }
                    }
                    composable(NavRoute.SettingsAccount.route) {
                        AccountSettingsScreen(settingsViewModel) { navController.popBackStack() }
                    }
                    composable(NavRoute.SettingsShared.route) {
                        SharedAccessSettingsScreen(settingsViewModel) { navController.popBackStack() }
                    }
                    composable(NavRoute.SettingsHost.route) {
                        HostSettingsScreen(settingsViewModel) { navController.popBackStack() }
                    }
                    composable(NavRoute.SettingsDeveloper.route) {
                        DeveloperSettingsScreen(settingsViewModel, shockerViewModel) { navController.popBackStack() }
                    }
                }

                InAppNotificationBanner(
                    notification = activeNotification,
                    onDismiss = {
                        shockerViewModel.dismissNotification()
                        settingsViewModel.dismissNotification()
                    },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}
