package com.prakash.pexplorer.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FileNameValidatorTest {
    @Test
    fun acceptsNormalNames() {
        assertNull(FileNameValidator.validate("Invoices 2026"))
    }

    @Test
    fun rejectsBlankAndDotNames() {
        assertEquals(FileNameError.EMPTY, FileNameValidator.validate("   "))
        assertEquals(FileNameError.DOT_NAME, FileNameValidator.validate(".."))
    }

    @Test
    fun rejectsPathSeparatorsAndControlCharacters() {
        assertEquals(FileNameError.INVALID_CHARACTER, FileNameValidator.validate("folder/name"))
        assertEquals(FileNameError.INVALID_CHARACTER, FileNameValidator.validate("bad\u0000name"))
    }
}
