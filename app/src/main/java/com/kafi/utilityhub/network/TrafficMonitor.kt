package com.kafi.utilityhub.network

import android.net.TrafficStats

class TrafficMonitor {
    data class Sample(val downBytesPerSec: Long, val upBytesPerSec: Long)

    private var lastRx = 0L
    private var lastTx = 0L
    private var lastTime = 0L

    fun sample(): Sample {
        val now = System.currentTimeMillis()
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()

        if (lastTime == 0L || rx == TrafficStats.UNSUPPORTED.toLong()) {
            lastRx = rx
            lastTx = tx
            lastTime = now
            return Sample(0, 0)
        }

        val elapsed = (now - lastTime).coerceAtLeast(1)
        val down = ((rx - lastRx).coerceAtLeast(0) * 1000 / elapsed)
        val up = ((tx - lastTx).coerceAtLeast(0) * 1000 / elapsed)

        lastRx = rx
        lastTx = tx
        lastTime = now
        return Sample(down, up)
    }
}