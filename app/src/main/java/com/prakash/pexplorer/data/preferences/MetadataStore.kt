package com.prakash.pexplorer.data.preferences

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.prakash.pexplorer.domain.model.ExplorerPreferences
import com.prakash.pexplorer.domain.model.FileKind
import com.prakash.pexplorer.domain.model.FileReference
import com.prakash.pexplorer.domain.model.SortOrder
import com.prakash.pexplorer.domain.model.ThemeMode
import com.prakash.pexplorer.domain.model.ViewStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.nio.charset.StandardCharsets

private val Context.pExplorerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "p_explorer_preferences"
)

class MetadataStore(context: Context) {
    private val dataStore = context.pExplorerDataStore

    val preferences: Flow<ExplorerPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::toPreferences)

    suspend fun setViewMode(viewStyle: ViewStyle) = dataStore.edit {
        it[Keys.VIEW_MODE] = viewStyle.name
    }

    suspend fun setSortOrder(sortOrder: SortOrder) = dataStore.edit {
        it[Keys.SORT_ORDER] = sortOrder.name
    }

    suspend fun setFoldersFirst(enabled: Boolean) = dataStore.edit {
        it[Keys.FOLDERS_FIRST] = enabled
    }

    suspend fun setShowHiddenFiles(enabled: Boolean) = dataStore.edit {
        it[Keys.SHOW_HIDDEN] = enabled
    }

    suspend fun setShowFileExtensions(enabled: Boolean) = dataStore.edit {
        it[Keys.SHOW_EXTENSIONS] = enabled
    }

    suspend fun setThemeMode(themeMode: ThemeMode) = dataStore.edit {
        it[Keys.THEME_MODE] = themeMode.name
    }

    suspend fun setConfirmBeforeDelete(enabled: Boolean) = dataStore.edit {
        it[Keys.CONFIRM_DELETE] = enabled
    }

    suspend fun setConfirmBeforeOverwrite(enabled: Boolean) = dataStore.edit {
        it[Keys.CONFIRM_OVERWRITE] = enabled
    }

    suspend fun setRememberLastFolder(enabled: Boolean) = dataStore.edit {
        it[Keys.REMEMBER_LAST_FOLDER] = enabled
    }

    suspend fun setRecentItemsEnabled(enabled: Boolean) = dataStore.edit { preferences ->
        preferences[Keys.RECENT_ITEMS_ENABLED] = enabled
        if (!enabled) preferences.remove(Keys.RECENT_FILES)
    }

    suspend fun setLastFolder(path: String?) = dataStore.edit {
        if (path == null) it.remove(Keys.LAST_FOLDER) else it[Keys.LAST_FOLDER] = path
    }

    suspend fun toggleFavorite(reference: FileReference) = dataStore.edit { preferences ->
        val references = decodeReferences(preferences[Keys.FAVORITES])
        val updated = if (references.any { it.path == reference.path }) {
            references.filterNot { it.path == reference.path }
        } else {
            listOf(reference) + references
        }
        preferences[Keys.FAVORITES] = encodeReferences(updated.take(MAX_FAVORITES))
    }

    suspend fun removeFavorite(path: String) = dataStore.edit { preferences ->
        preferences[Keys.FAVORITES] = encodeReferences(
            decodeReferences(preferences[Keys.FAVORITES]).filterNot { it.path == path }
        )
    }

    suspend fun recordRecent(reference: FileReference) = dataStore.edit { preferences ->
        val updated = listOf(reference) + decodeReferences(preferences[Keys.RECENT_FILES])
            .filterNot { it.path == reference.path }
        preferences[Keys.RECENT_FILES] = encodeReferences(updated.take(MAX_RECENT))
    }

    suspend fun removeRecent(path: String) = dataStore.edit { preferences ->
        preferences[Keys.RECENT_FILES] = encodeReferences(
            decodeReferences(preferences[Keys.RECENT_FILES]).filterNot { it.path == path }
        )
    }

    suspend fun clearRecent() = dataStore.edit { preferences ->
        preferences.remove(Keys.RECENT_FILES)
    }

    suspend fun recordSearch(query: String) = dataStore.edit { preferences ->
        val normalized = query.trim()
        if (normalized.isEmpty()) return@edit
        val updated = listOf(normalized) + decodeStrings(preferences[Keys.SEARCH_HISTORY])
            .filterNot { it.equals(normalized, ignoreCase = true) }
        preferences[Keys.SEARCH_HISTORY] = encodeStrings(updated.take(MAX_SEARCH_HISTORY))
    }

    suspend fun clearSearchHistory() = dataStore.edit { preferences ->
        preferences.remove(Keys.SEARCH_HISTORY)
    }

    private fun toPreferences(preferences: Preferences): ExplorerPreferences = ExplorerPreferences(
        viewStyle = enumValue(preferences[Keys.VIEW_MODE], ViewStyle.LIST),
        sortOrder = enumValue(preferences[Keys.SORT_ORDER], SortOrder.NAME_ASC),
        foldersFirst = preferences[Keys.FOLDERS_FIRST] ?: true,
        showHiddenFiles = preferences[Keys.SHOW_HIDDEN] ?: false,
        showFileExtensions = preferences[Keys.SHOW_EXTENSIONS] ?: true,
        themeMode = enumValue(preferences[Keys.THEME_MODE], ThemeMode.SYSTEM),
        confirmBeforeDelete = preferences[Keys.CONFIRM_DELETE] ?: true,
        confirmBeforeOverwrite = preferences[Keys.CONFIRM_OVERWRITE] ?: true,
        rememberLastFolder = preferences[Keys.REMEMBER_LAST_FOLDER] ?: true,
        recentItemsEnabled = preferences[Keys.RECENT_ITEMS_ENABLED] ?: true,
        lastFolder = preferences[Keys.LAST_FOLDER],
        favorites = decodeReferences(preferences[Keys.FAVORITES]),
        recentFiles = decodeReferences(preferences[Keys.RECENT_FILES]),
        searchHistory = decodeStrings(preferences[Keys.SEARCH_HISTORY])
    )

    private inline fun <reified T : Enum<T>> enumValue(value: String?, fallback: T): T =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

    private fun encodeReferences(references: List<FileReference>): String =
        references.joinToString("\n") { reference ->
            listOf(
                reference.path,
                reference.name,
                reference.isDirectory.toString(),
                reference.kind.name,
                reference.mimeType.orEmpty(),
                reference.lastAccessedEpochMillis?.toString().orEmpty()
            ).joinToString("|") { encode(it) }
        }

    private fun decodeReferences(value: String?): List<FileReference> =
        value.orEmpty().lineSequence().mapNotNull { line ->
            val fields = line.split('|')
            if (fields.size != 6) return@mapNotNull null
            runCatching {
                FileReference(
                    path = decode(fields[0]),
                    name = decode(fields[1]),
                    isDirectory = decode(fields[2]).toBoolean(),
                    kind = FileKind.valueOf(decode(fields[3])),
                    mimeType = decode(fields[4]).ifBlank { null },
                    lastAccessedEpochMillis = decode(fields[5]).toLongOrNull()
                )
            }.getOrNull()
        }.toList()

    private fun encodeStrings(values: List<String>): String =
        values.joinToString("\n", transform = ::encode)

    private fun decodeStrings(value: String?): List<String> =
        value.orEmpty().lineSequence().mapNotNull { encoded ->
            encoded.takeIf { it.isNotBlank() }?.let(::decode)
        }.toList()

    private fun encode(value: String): String =
        Base64.encodeToString(value.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)

    private fun decode(value: String): String =
        String(Base64.decode(value, Base64.NO_WRAP), StandardCharsets.UTF_8)

    private object Keys {
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val FOLDERS_FIRST = booleanPreferencesKey("folders_first")
        val SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
        val SHOW_EXTENSIONS = booleanPreferencesKey("show_extensions")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CONFIRM_DELETE = booleanPreferencesKey("confirm_delete")
        val CONFIRM_OVERWRITE = booleanPreferencesKey("confirm_overwrite")
        val REMEMBER_LAST_FOLDER = booleanPreferencesKey("remember_last_folder")
        val RECENT_ITEMS_ENABLED = booleanPreferencesKey("recent_items_enabled")
        val LAST_FOLDER = stringPreferencesKey("last_folder")
        val FAVORITES = stringPreferencesKey("favorites")
        val RECENT_FILES = stringPreferencesKey("recent_files")
        val SEARCH_HISTORY = stringPreferencesKey("search_history")
    }

    private companion object {
        const val MAX_FAVORITES = 500
        const val MAX_RECENT = 50
        const val MAX_SEARCH_HISTORY = 20
    }
}
