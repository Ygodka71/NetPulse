package com.example.data.network

import com.example.model.SpeedMeasurementResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min

class SpeedTestService(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    private val downloadUrls = listOf(
        "https://speed.cloudflare.com/__down?bytes=15000000",
        "https://proof.ovh.net/files/10Mb.dat"
    )

    private val uploadUrls = listOf(
        "https://speed.cloudflare.com/__up",
        "https://httpbin.org/post"
    )

    suspend fun runDownloadTest(
        onProgress: (currentMbps: Double, peakMbps: Double, progress: Float) -> Unit
    ): SpeedMeasurementResult = withContext(Dispatchers.IO) {
        var finalResult = SpeedMeasurementResult()

        for (url in downloadUrls) {
            if (!coroutineContext.isActive) break
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Cache-Control", "no-cache")
                    .build()

                val call = httpClient.newCall(request)
                val response = call.execute()
                val body = response.body ?: continue
                val totalLength = body.contentLength().takeIf { it > 0 } ?: 15_000_000L

                val inputStream: InputStream = body.byteStream()
                val buffer = ByteArray(32 * 1024)

                var totalBytesRead = 0L
                var peakSpeed = 0.0
                var minSpeed = Double.MAX_VALUE

                val overallStartNs = System.nanoTime()
                var intervalStartNs = overallStartNs
                var intervalBytesRead = 0L

                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (!coroutineContext.isActive) {
                        call.cancel()
                        break
                    }

                    totalBytesRead += bytesRead
                    intervalBytesRead += bytesRead

                    val nowNs = System.nanoTime()
                    val intervalDeltaSec = (nowNs - intervalStartNs) / 1_000_000_000.0

                    if (intervalDeltaSec >= 0.1) {
                        val instantMbps = (intervalBytesRead * 8.0) / (intervalDeltaSec * 1_000_000.0)
                        if (instantMbps > peakSpeed) {
                            peakSpeed = instantMbps
                        }
                        if (instantMbps > 0.1 && instantMbps < minSpeed) {
                            minSpeed = instantMbps
                        }
                        val progress = (totalBytesRead.toFloat() / totalLength).coerceIn(0f, 1f)
                        onProgress(instantMbps, peakSpeed, progress)

                        intervalStartNs = nowNs
                        intervalBytesRead = 0L
                    }
                }

                val overallDurationSec = (System.nanoTime() - overallStartNs) / 1_000_000_000.0
                body.close()

                if (totalBytesRead > 100_000 && overallDurationSec > 0.3) {
                    val avgSpeed = (totalBytesRead * 8.0) / (overallDurationSec * 1_000_000.0)
                    if (minSpeed == Double.MAX_VALUE) minSpeed = avgSpeed
                    onProgress(avgSpeed, max(peakSpeed, avgSpeed), 1f)
                    finalResult = SpeedMeasurementResult(
                        finalAvgMbps = avgSpeed,
                        peakMbps = max(peakSpeed, avgSpeed),
                        minMbps = min(minSpeed, avgSpeed)
                    )
                    return@withContext finalResult
                }
            } catch (_: Exception) {
                if (!coroutineContext.isActive) break
            }
        }

        return@withContext finalResult
    }

    suspend fun runUploadTest(
        onProgress: (currentMbps: Double, peakMbps: Double, progress: Float) -> Unit
    ): SpeedMeasurementResult = withContext(Dispatchers.IO) {
        var finalResult = SpeedMeasurementResult()
        val targetBytes = 6_000_000L // 6 MB payload

        for (url in uploadUrls) {
            if (!coroutineContext.isActive) break
            try {
                val overallStartNs = System.nanoTime()
                var peakSpeed = 0.0
                var minSpeed = Double.MAX_VALUE

                val progressBody = object : RequestBody() {
                    override fun contentType() = "application/octet-stream".toMediaType()

                    override fun contentLength() = targetBytes

                    override fun writeTo(sink: BufferedSink) {
                        val chunkSize = 32 * 1024
                        val chunk = ByteArray(chunkSize) { 0x5A }
                        var bytesWritten = 0L

                        var intervalStartNs = System.nanoTime()
                        var intervalBytesWritten = 0L

                        while (bytesWritten < targetBytes) {
                            val toWrite = minOf(chunkSize.toLong(), targetBytes - bytesWritten).toInt()
                            sink.write(chunk, 0, toWrite)
                            bytesWritten += toWrite
                            intervalBytesWritten += toWrite

                            val nowNs = System.nanoTime()
                            val intervalDeltaSec = (nowNs - intervalStartNs) / 1_000_000_000.0

                            if (intervalDeltaSec >= 0.1) {
                                val instantMbps = (intervalBytesWritten * 8.0) / (intervalDeltaSec * 1_000_000.0)
                                if (instantMbps > peakSpeed) {
                                    peakSpeed = instantMbps
                                }
                                if (instantMbps > 0.1 && instantMbps < minSpeed) {
                                    minSpeed = instantMbps
                                }
                                val progress = (bytesWritten.toFloat() / targetBytes).coerceIn(0f, 1f)
                                onProgress(instantMbps, peakSpeed, progress)

                                intervalStartNs = nowNs
                                intervalBytesWritten = 0L
                            }
                        }
                    }
                }

                val request = Request.Builder()
                    .url(url)
                    .post(progressBody)
                    .build()

                val call = httpClient.newCall(request)
                val response = call.execute()
                response.close()

                val overallDurationSec = (System.nanoTime() - overallStartNs) / 1_000_000_000.0
                if (overallDurationSec > 0.3) {
                    val avgSpeed = (targetBytes * 8.0) / (overallDurationSec * 1_000_000.0)
                    if (minSpeed == Double.MAX_VALUE) minSpeed = avgSpeed
                    onProgress(avgSpeed, max(peakSpeed, avgSpeed), 1f)
                    finalResult = SpeedMeasurementResult(
                        finalAvgMbps = avgSpeed,
                        peakMbps = max(peakSpeed, avgSpeed),
                        minMbps = min(minSpeed, avgSpeed)
                    )
                    return@withContext finalResult
                }
            } catch (_: Exception) {
                if (!coroutineContext.isActive) break
            }
        }

        return@withContext finalResult
    }
}
