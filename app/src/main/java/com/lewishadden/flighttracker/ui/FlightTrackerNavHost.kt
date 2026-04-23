package com.lewishadden.flighttracker.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lewishadden.flighttracker.ui.detail.FlightDetailScreen
import com.lewishadden.flighttracker.ui.flightmap.FlightMapScreen
import com.lewishadden.flighttracker.ui.predownload.PreDownloadScreen
import com.lewishadden.flighttracker.ui.search.SearchScreen

object Routes {
    const val SEARCH = "search"
    const val DETAIL = "detail/{faFlightId}"
    const val PREDOWNLOAD = "predownload/{faFlightId}"
    const val MAP = "map/{faFlightId}"
    fun detail(faFlightId: String) = "detail/$faFlightId"
    fun predownload(faFlightId: String) = "predownload/$faFlightId"
    fun map(faFlightId: String) = "map/$faFlightId"
}

@Composable
fun FlightTrackerNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.SEARCH) {
        composable(Routes.SEARCH) {
            SearchScreen(
                vm = hiltViewModel(),
                onOpenFlight = { nav.navigate(Routes.detail(it)) },
            )
        }
        composable(
            Routes.DETAIL,
            arguments = listOf(navArgument("faFlightId") { type = NavType.StringType }),
        ) {
            FlightDetailScreen(
                vm = hiltViewModel(),
                onPreDownload = { id -> nav.navigate(Routes.predownload(id)) },
                onOpenMap = { id -> nav.navigate(Routes.map(id)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.PREDOWNLOAD,
            arguments = listOf(navArgument("faFlightId") { type = NavType.StringType }),
        ) {
            PreDownloadScreen(
                vm = hiltViewModel(),
                onFinished = { id -> nav.navigate(Routes.map(id)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.MAP,
            arguments = listOf(navArgument("faFlightId") { type = NavType.StringType }),
        ) {
            FlightMapScreen(
                vm = hiltViewModel(),
                onBack = { nav.popBackStack() },
            )
        }
    }
}
