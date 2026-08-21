package com.prakash.pexplorer.data.repository

import com.prakash.pexplorer.data.preferences.MetadataStore
import com.prakash.pexplorer.domain.model.ExplorerPreferences
import com.prakash.pexplorer.domain.model.FileReference
import com.prakash.pexplorer.domain.model.SortOrder
import com.prakash.pexplorer.domain.model.ThemeMode
import com.prakash.pexplorer.domain.model.ViewStyle
import kotlinx.coroutines.flow.Flow

class MetadataRepository(
    private val store: MetadataStore
) {
    val preferences: Flow<ExplorerPreferences> = store.preferences

    suspend fun setViewMode(viewStyle: ViewStyle) = store.setViewMode(viewStyle)
    suspend fun setSortOrder(sortOrder: SortOrder) = store.setSortOrder(sortOrder)
    suspend fun setFoldersFirst(enabled: Boolean) = store.setFoldersFirst(enabled)
    suspend fun setShowHiddenFiles(enabled: Boolean) = store.setShowHiddenFiles(enabled)
    suspend fun setShowFileExtensions(enabled: Boolean) = store.setShowFileExtensions(enabled)
    suspend fun setThemeMode(themeMode: ThemeMode) = store.setThemeMode(themeMode)
    suspend fun setConfirmBeforeDelete(enabled: Boolean) = store.setConfirmBeforeDelete(enabled)
    suspend fun setConfirmBeforeOverwrite(enabled: Boolean) = store.setConfirmBeforeOverwrite(enabled)
    suspend fun setRememberLastFolder(enabled: Boolean) = store.setRememberLastFolder(enabled)
    suspend fun setRecentItemsEnabled(enabled: Boolean) = store.setRecentItemsEnabled(enabled)
    suspend fun setLastFolder(path: String?) = store.setLastFolder(path)
    suspend fun toggleFavorite(reference: FileReference) = store.toggleFavorite(reference)
    suspend fun removeFavorite(path: String) = store.removeFavorite(path)
    suspend fun recordRecent(reference: FileReference) = store.recordRecent(reference)
    suspend fun removeRecent(path: String) = store.removeRecent(path)
    suspend fun clearRecent() = store.clearRecent()
    suspend fun recordSearch(query: String) = store.recordSearch(query)
    suspend fun clearSearchHistory() = store.clearSearchHistory()
}
