package com.prakash.pexplorer.domain.model

data class ExplorerPreferences(
    val viewStyle: ViewStyle = ViewStyle.LIST,
    val sortOrder: SortOrder = SortOrder.NAME_ASC,
    val foldersFirst: Boolean = true,
    val showHiddenFiles: Boolean = false,
    val showFileExtensions: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val confirmBeforeDelete: Boolean = true,
    val confirmBeforeOverwrite: Boolean = true,
    val rememberLastFolder: Boolean = true,
    val recentItemsEnabled: Boolean = true,
    val lastFolder: String? = null,
    val favorites: List<FileReference> = emptyList(),
    val recentFiles: List<FileReference> = emptyList(),
    val searchHistory: List<String> = emptyList()
)
