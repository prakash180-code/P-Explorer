package com.prakash.pexplorer.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PHubJsonParserTest {
    @Test
    fun parsesChallengeCodes() {
        assertEquals(
            listOf("12", "34", "56"),
            PHubJsonParser.mapChallengeCodes(listOf("12", "", "34", "56"))
        )
    }

    @Test
    fun parsesDirectoriesAndFilesWithDefaults() {
        val entries = PHubJsonParser.mapEntries(
            listOf(
                PHubJsonParser.RawEntry("Music", "/Music", true, 0L, null),
                PHubJsonParser.RawEntry("song.mp3", "/Music/song.mp3", false, 123L, null)
            )
        )

        assertEquals(2, entries.size)
        assertTrue(entries.first().isDirectory)
        assertEquals(123L, entries.last().sizeBytes)
        assertEquals(null, entries.last().modifiedEpochMillis)
    }
}
