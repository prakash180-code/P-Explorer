package com.prakash.pexplorer.presentation

import com.prakash.pexplorer.domain.model.ExplorerFile

data class PreviewUiState(
    val file: ExplorerFile,
    val text: String? = null,
    val isLoading: Boolean = false,
    val isTruncated: Boolean = false,
    val errorMessage: String? = null
)
