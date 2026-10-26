package com.example.data.network

import com.example.model.PingStatistics
import com.example.model.PingTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToLong

class PingService(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()
) {

    private val samples = mutableListOf<Long>()
    private var totalSent = 0
    private var lostCount = 0

    fun reset() {
        samples.clear()
        totalSent = 0
        lostCount = 0
    }

    suspend fun measureSinglePing(target: PingTarget): Long? = withContext(Dispatchers.IO) {
        // Attempt 1: TCP Socket to Port (DNS 53 or 80)
        try {
            val socket = Socket()
            val startNs = System.nanoTime()
            socket.connect(InetSocketAddress(target.host, target.port), 2500)
            val durationMs = ((System.nanoTime() - startNs) / 1_000_000.0).roundToLong()
            socket.close()
            return@withContext durationMs.coerceAtLeast(1L)
        } catch (_: Exception) {
            // Attempt 2: Fallback to fast HTTP endpoint
            try {
                val request = Request.Builder()
                    .url(target.httpFallbackUrl)
                    .head()
                    .build()
                val startNs = System.nanoTime()
                val response = httpClient.newCall(request).execute()
                response.close()
                val durationMs = ((System.nanoTime() - startNs) / 1_000_000.0).roundToLong()
                return@withContext durationMs.coerceAtLeast(1L)
            } catch (_: Exception) {
                return@withContext null
            }
        }
    }

    fun recordPing(latencyMs: Long?, maxHistoryWindow: Int = 30): PingStatistics {
        totalSent++
        if (latencyMs == null) {
            lostCount++
        } else {
            samples.add(latencyMs)
            if (samples.size > maxHistoryWindow) {
                samples.removeAt(0)
            }
        }
        return computeStatistics(latencyMs)
    }

    fun resetPacketLoss(): PingStatistics {
        totalSent = 0
        lostCount = 0
        return computeStatistics(samples.lastOrNull())
    }

    private fun computeStatistics(currentLatency: Long?): PingStatistics {
        val validSamples = samples.toList()
        val current = currentLatency ?: (validSamples.lastOrNull() ?: 0L)
        val avg = if (validSamples.isNotEmpty()) validSamples.average().roundToLong() else 0L
        val min = validSamples.minOrNull() ?: 0L
        val max = validSamples.maxOrNull() ?: 0L

        // Jitter: Average variation between consecutive samples
        val jitter = if (validSamples.size >= 2) {
            var diffSum = 0L
            for (i in 1 until validSamples.size) {
                diffSum += abs(validSamples[i] - validSamples[i - 1])
            }
            (diffSum.toDouble() / (validSamples.size - 1)).roundToLong()
        } else {
            0L
        }

        val lossPercent = if (totalSent > 0) ((lostCount.toDouble() / totalSent) * 100).toInt() else 0

        val floatSamples = validSamples.map { it.toFloat() }

        return PingStatistics(
            currentPingMs = current,
            avgLatencyMs = avg,
            minLatencyMs = min,
            maxLatencyMs = max,
            jitterMs = jitter,
            packetLossPercent = lossPercent,
            historySamples = floatSamples,
            totalPingsSent = totalSent
        )
    }
}
