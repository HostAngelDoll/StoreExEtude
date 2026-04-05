package dev.anilbeesetti.nextplayer.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import dev.anilbeesetti.nextplayer.settings.screens.journals.JournalPreferencesRoute

const val journalPreferencesNavigationRoute = "journal_preferences_route"

fun NavController.navigateToJournalPreferences(navOptions: NavOptions? = null) {
    this.navigate(journalPreferencesNavigationRoute, navOptions)
}

fun NavGraphBuilder.journalPreferencesScreen(onNavigateUp: () -> Unit) {
    composable(route = journalPreferencesNavigationRoute) {
        JournalPreferencesRoute(onNavigateUp = onNavigateUp)
    }
}
