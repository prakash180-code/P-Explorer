package com.prakash.pexplorer.domain.usecase

enum class FileNameError {
    EMPTY,
    DOT_NAME,
    INVALID_CHARACTER
}

object FileNameValidator {
    private val invalidCharacters = setOf('<', '>', ':', '"', '/', '\\', '|', '?', '*')

    fun validate(name: String): FileNameError? {
        if (name.isBlank()) return FileNameError.EMPTY
        if (name == "." || name == "..") return FileNameError.DOT_NAME
        if (name.any { it.code < 32 || it in invalidCharacters }) {
            return FileNameError.INVALID_CHARACTER
        }
        return null
    }
}
