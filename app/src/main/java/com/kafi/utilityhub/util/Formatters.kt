package com.kafi.utilityhub.util

import java.util.Locale

object Formatters {
    fun bytesPerSec(bps: Long): String {
        val v = bps.toDouble()
        return when {
            v >= 1024.0 * 1024.0 -> String.format(Locale.US, "%.2f MB/s", v / (1024.0 * 1024.0))
            v >= 1024.0 -> String.format(Locale.US, "%.1f KB/s", v / 1024.0)
            else -> String.format(Locale.US, "%d B/s", bps)
        }
    }

    fun bytes(total: Long): String {
        val v = total.toDouble()
        return when {
            v >= 1024.0 * 1024.0 * 1024.0 -> String.format(Locale.US, "%.2f GB", v / (1024.0 * 1024.0 * 1024.0))
            v >= 1024.0 * 1024.0 -> String.format(Locale.US, "%.2f MB", v / (1024.0 * 1024.0))
            v >= 1024.0 -> String.format(Locale.US, "%.1f KB", v / 1024.0)
            else -> "$total B"
        }
    }

    fun duration(ms: Long): String {
        val s = ms / 1000
        val m = s / 60
        return String.format(Locale.US, "%02d:%02d", m, s % 60)
    }
}
