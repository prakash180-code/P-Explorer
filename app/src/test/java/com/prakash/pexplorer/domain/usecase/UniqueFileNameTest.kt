package com.prakash.pexplorer.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class UniqueFileNameTest {
    @Test
    fun addsIncrementingSuffixBeforeExtension() {
        val existing = setOf("invoice.pdf", "invoice (1).pdf")

        val result = UniqueFileName.nextAvailableName("invoice.pdf") { it in existing }

        assertEquals("invoice (2).pdf", result)
    }

    @Test
    fun addsSuffixToFolderNames() {
        val result = UniqueFileName.nextAvailableName("Documents") { it == "Documents" }

        assertEquals("Documents (1)", result)
    }
}
