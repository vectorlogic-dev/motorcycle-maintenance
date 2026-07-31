package com.motocare.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {
    @Test
    fun `money parsing accepts at most two decimal places`() {
        assertEquals(12_345L, "123.45".toCentavosOrNull())
        assertEquals(12_300L, "123".toCentavosOrNull())
        assertNull("123.456".toCentavosOrNull())
        assertNull("-1".toCentavosOrNull())
    }
}
