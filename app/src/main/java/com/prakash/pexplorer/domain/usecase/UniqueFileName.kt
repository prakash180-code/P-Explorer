package com.prakash.pexplorer.domain.usecase

object UniqueFileName {
    fun nextAvailableName(name: String, exists: (String) -> Boolean): String {
        if (!exists(name)) return name

        val extension = name.substringAfterLast('.', missingDelimiterValue = "")
        val baseName = if (extension.isNotEmpty() && !name.startsWith('.')) {
            name.removeSuffix(".$extension")
        } else {
            name
        }
        var index = 1
        while (true) {
            val candidate = if (extension.isNotEmpty() && !name.startsWith('.')) {
                "$baseName ($index).$extension"
            } else {
                "$baseName ($index)"
            }
            if (!exists(candidate)) return candidate
            index++
        }
    }
}
