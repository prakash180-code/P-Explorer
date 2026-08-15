package com.prakash.pexplorer.data.network

import com.prakash.pexplorer.domain.model.NetworkFileEntry
import org.json.JSONArray

object PHubJsonParser {
    fun parseChallenge(response: String): List<String> {
        val array = JSONArray(response)
        return mapChallengeCodes(buildList {
            for (index in 0 until array.length()) add(array.getString(index))
        })
    }

    fun parseEntries(response: String): List<NetworkFileEntry> {
        val array = JSONArray(response)
        return mapEntries(buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    RawEntry(
                        name = item.optString("name"),
                        path = item.optString("path"),
                        isDirectory = item.optBoolean("isDir", false),
                        sizeBytes = item.optLong("size", 0L),
                        modifiedEpochMillis = item.optLong("time", 0L).takeIf { it > 0L }
                    )
                )
            }
        })
    }

    fun mapChallengeCodes(codes: List<String>): List<String> = codes.filter { it.isNotBlank() }

    fun mapEntries(entries: List<RawEntry>): List<NetworkFileEntry> = entries.map { entry ->
        NetworkFileEntry(
            name = entry.name,
            path = entry.path,
            isDirectory = entry.isDirectory,
            sizeBytes = entry.sizeBytes,
            modifiedEpochMillis = entry.modifiedEpochMillis
        )
    }

    data class RawEntry(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val sizeBytes: Long,
        val modifiedEpochMillis: Long?
    )
}
