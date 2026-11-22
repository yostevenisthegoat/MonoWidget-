package com.example.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppThemeMode
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyWidgetsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.WidgetLiteTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel
import com.example.data.model.WidgetConfig

@Composable
fun WidgetLiteApp(
    viewModel: MainViewModel,
    initialConfigId: Long = -1L
) {
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val savedWidgets by viewModel.savedWidgets.collectAsStateWithLifecycle()
    val studioConfig by viewModel.studioConfig.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val nowMillis by viewModel.nowMillis.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val cityResults by viewModel.cityResults.collectAsStateWithLifecycle()
    val isWeatherLoading by viewModel.isWeatherLoading.collectAsStateWithLifecycle()
    val launchableApps by viewModel.launchableApps.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppTab.HOME.route

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(initialConfigId) {
        if (initialConfigId > 0L) {
            viewModel.openConfigById(initialConfigId)
            navController.navigate(AppTab.STUDIO.route) {
                launchSingleTop = true
            }
        }
    }

    val isDarkTheme = when (settings.themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    fun navigateToTab(tab: AppTab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    WidgetLiteTheme(
        themeMode = settings.themeMode,
        accentColor = settings.accentColor
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar {
                            AppTab.entries.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab, selected),
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_${tab.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail {
                            AppTab.entries.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab, selected),
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("rail_${tab.route}")
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = AppTab.HOME.route,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(AppTab.HOME.route) {
                            HomeScreen(
                                searchQuery = searchQuery,
                                recentTypeIds = settings.recentTypeIds,
                                telemetry = telemetry,
                                nowMillis = nowMillis,
                                isDarkTheme = isDarkTheme,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onOpenTypeInStudio = { type ->
                                    viewModel.openTypeInStudio(type, isDarkTheme)
                                    navigateToTab(AppTab.STUDIO)
                                },
                                onOpenCategoryInExplore = { cat ->
                                    viewModel.selectCategory(cat)
                                    navigateToTab(AppTab.EXPLORE)
                                },
                                onOpenExplore = { navigateToTab(AppTab.EXPLORE) }
                            )
                        }

                        composable(AppTab.EXPLORE.route) {
                            ExploreScreen(
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                telemetry = telemetry,
                                nowMillis = nowMillis,
                                isDarkTheme = isDarkTheme,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onSelectCategory = viewModel::selectCategory,
                                onCustomizeType = { type ->
                                    viewModel.openTypeInStudio(type, isDarkTheme)
                                    navigateToTab(AppTab.STUDIO)
                                },
                                onQuickPinType = { type ->
                                    val cfg = WidgetConfig.createDefault(type, isDarkTheme)
                                    viewModel.pinConfigToHome(cfg)
                                }
                            )
                        }

                        composable(AppTab.MY_WIDGETS.route) {
                            MyWidgetsScreen(
                                widgets = savedWidgets,
                                telemetry = telemetry,
                                nowMillis = nowMillis,
                                onEditWidget = { cfg ->
                                    viewModel.openConfigInStudio(cfg)
                                    navigateToTab(AppTab.STUDIO)
                                },
                                onPinWidget = viewModel::pinConfigToHome,
                                onDuplicateWidget = viewModel::duplicateWidget,
                                onDeleteWidget = viewModel::deleteWidget,
                                onBrowseExplore = { navigateToTab(AppTab.EXPLORE) }
                            )
                        }

                        composable(AppTab.STUDIO.route) {
                            StudioScreen(
                                config = studioConfig,
                                telemetry = telemetry,
                                nowMillis = nowMillis,
                                isDarkTheme = isDarkTheme,
                                cityResults = cityResults,
                                isWeatherLoading = isWeatherLoading,
                                launchableApps = launchableApps,
                                onUpdateConfig = viewModel::updateStudioConfig,
                                onUpdatePayload = viewModel::updateStudioPayload,
                                onInteractPreview = viewModel::handlePreviewInteraction,
                                onPickPhoto = viewModel::importPhotoFromPicker,
                                onSearchCities = viewModel::searchCities,
                                onSelectCity = viewModel::selectCityAndFetchWeather,
                                onRefreshWeather = viewModel::refreshWeatherForStudio,
                                onResetDefault = { viewModel.resetStudioToDefault(isDarkTheme) },
                                onSave = viewModel::saveStudioWidget,
                                onPinToHome = viewModel::pinStudioWidgetToHome,
                                onRequestPermissionMessage = viewModel::emitMessage
                            )
                        }

                        composable(AppTab.SETTINGS.route) {
                            SettingsScreen(
                                settings = settings,
                                onSetThemeMode = viewModel::setThemeMode,
                                onSetAccentColor = viewModel::setAccentColor,
                                onSetAllowWeatherNetwork = viewModel::setAllowWeatherNetwork,
                                onBuildExportJson = viewModel::buildExportJson,
                                onImportJson = viewModel::importFromJson,
                                onClearLocalPhotos = viewModel::clearLocalPhotos,
                                onRefreshWidgets = viewModel::refreshTelemetryAndWidgets,
                                onNotify = viewModel::emitMessage
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: AppTab, selected: Boolean) = when (tab) {
    AppTab.HOME -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
    AppTab.EXPLORE -> if (selected) Icons.Rounded.Explore else Icons.Outlined.Explore
    AppTab.MY_WIDGETS -> if (selected) Icons.Rounded.Widgets else Icons.Outlined.Widgets
    AppTab.STUDIO -> if (selected) Icons.Rounded.Tune else Icons.Outlined.Tune
    AppTab.SETTINGS -> if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings
}
