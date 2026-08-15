package com.prakash.pexplorer.domain.model

data class SearchUiState(
    val query: String = "",
    val results: List<ExplorerFile> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null
)
