package com.lewishadden.flighttracker.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lewishadden.flighttracker.ui.airport.AirportScreen
import com.lewishadden.flighttracker.ui.detail.FlightDetailScreen
import com.lewishadden.flighttracker.ui.flightmap.FlightMapScreen
import com.lewishadden.flighttracker.ui.main.MainTabsScaffold
import com.lewishadden.flighttracker.ui.predownload.PreDownloadScreen
import com.lewishadden.flighttracker.ui.weather.RouteWeatherScreen

object Routes {
    const val HOME = "home"
    const val DETAIL = "detail/{faFlightId}"
    const val PREDOWNLOAD = "predownload/{faFlightId}"
    const val MAP = "map/{faFlightId}"
    const val AIRPORT = "airport/{code}"
    const val ROUTE_WEATHER = "weather/{faFlightId}"
    fun detail(faFlightId: String) = "detail/$faFlightId"
    fun predownload(faFlightId: String) = "predownload/$faFlightId"
    fun map(faFlightId: String) = "map/$faFlightId"
    fun airport(code: String) = "airport/$code"
    fun routeWeather(faFlightId: String) = "weather/$faFlightId"
}

@Composable
fun FlightTrackerNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            MainTabsScaffold(
                onOpenFlight = { nav.navigate(Routes.detail(it)) },
                onOpenMap = { nav.navigate(Routes.map(it)) },
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
                onOpenAirport = { code -> nav.navigate(Routes.airport(code)) },
                onOpenRouteWeather = { id -> nav.navigate(Routes.routeWeather(id)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.ROUTE_WEATHER,
            arguments = listOf(navArgument("faFlightId") { type = NavType.StringType }),
        ) {
            RouteWeatherScreen(
                vm = hiltViewModel(),
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.AIRPORT,
            arguments = listOf(navArgument("code") { type = NavType.StringType }),
        ) {
            AirportScreen(
                vm = hiltViewModel(),
                onBack = { nav.popBackStack() },
                onOpenFlight = { id -> nav.navigate(Routes.detail(id)) },
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
