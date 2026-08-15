package com.prakash.pexplorer.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.prakash.pexplorer.R

const val SEARCH_ROUTE = "search"
const val PREVIEW_ROUTE = "preview"
const val LARGE_FILES_ROUTE = "utility/large-files"
const val DUPLICATES_ROUTE = "utility/duplicates"

enum class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
) {
    HOME("home", R.string.home, Icons.Filled.Home),
    FILES("files", R.string.files, Icons.Filled.FolderOpen),
    RECENT("recent", R.string.recent, Icons.AutoMirrored.Filled.ReceiptLong),
    FAVORITES("favorites", R.string.favorites, Icons.Filled.Favorite)
}

enum class UtilityDestination(
    val route: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector
) {
    ANALYZER("utility/analyzer", R.string.storage_analyzer, R.string.utility_analyzer_description, Icons.Filled.Storage),
    P_HUB("utility/p-hub", R.string.p_hub, R.string.network_protocol_note, Icons.Filled.Wifi),
    SETTINGS("utility/settings", R.string.settings, R.string.utility_settings_description, Icons.Filled.Settings),
    ABOUT("utility/about", R.string.about, R.string.utility_about_description, Icons.Filled.Info)
}
