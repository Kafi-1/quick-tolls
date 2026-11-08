package com.kafi.utilityhub

import com.kafi.utilityhub.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun bytesPerSec_formatsBytesKiloAndMega() {
        assertEquals("512 B/s", Formatters.bytesPerSec(512))
        assertEquals("1.5 KB/s", Formatters.bytesPerSec(1536))
        assertEquals("2.00 MB/s", Formatters.bytesPerSec(2L * 1024 * 1024))
    }

    @Test
    fun bytes_formatsUnits() {
        assertEquals("512 B", Formatters.bytes(512))
        assertEquals("2.0 KB", Formatters.bytes(2048))
        assertEquals("1.50 MB", Formatters.bytes((1.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun duration_formatsMinutesAndSeconds() {
        assertEquals("00:00", Formatters.duration(0))
        assertEquals("01:05", Formatters.duration(65_000))
        assertEquals("10:00", Formatters.duration(600_000))
    }
}