package com.lewishadden.flighttracker.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.lewishadden.flighttracker.ui.offline.OfflineFlightsScreen
import com.lewishadden.flighttracker.ui.search.SearchScreen
import com.lewishadden.flighttracker.ui.settings.SettingsScreen
import com.lewishadden.flighttracker.ui.subscribed.WatchedFlightsScreen
import com.lewishadden.flighttracker.ui.theme.Brand
import com.lewishadden.flighttracker.ui.usage.AccountUsageScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    SEARCH("Search", Icons.Default.Search),
    WATCHED("Watched", Icons.Default.NotificationsActive),
    OFFLINE("Offline", Icons.Default.CloudDownload),
    USAGE("Usage", Icons.Default.DataUsage),
    SETTINGS("Settings", Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabsScaffold(
    onOpenFlight: (String) -> Unit,
    onOpenMap: (String) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(Tab.SEARCH) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Brand.IndigoDeep,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { selected = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Brand.IndigoHi,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    ) { pad ->
        Box(modifier = Modifier.fillMaxSize().padding(pad)) {
            when (selected) {
                Tab.SEARCH -> SearchScreen(
                    vm = hiltViewModel(),
                    onOpenFlight = onOpenFlight,
                )
                Tab.WATCHED -> WatchedFlightsScreen(
                    vm = hiltViewModel(),
                    onOpenFlight = onOpenFlight,
                )
                Tab.OFFLINE -> OfflineFlightsScreen(
                    vm = hiltViewModel(),
                    onOpenMap = onOpenMap,
                    onOpenDetail = onOpenFlight,
                )
                Tab.USAGE -> AccountUsageScreen(vm = hiltViewModel())
                Tab.SETTINGS -> SettingsScreen(vm = hiltViewModel())
            }
        }
    }
}
