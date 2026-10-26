package com.example.data.network

import com.example.model.PublicIpInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PublicIpService(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()
) {
    private var cachedInfo: PublicIpInfo? = null

    suspend fun getPublicIpInfo(forceRefresh: Boolean = false): PublicIpInfo? = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedInfo != null) {
            return@withContext cachedInfo
        }

        // Strategy 1: Cloudflare trace (fastest, highly reliable, gives ip & 2-letter country code loc)
        try {
            val request = Request.Builder()
                .url("https://cloudflare.com/cdn-cgi/trace")
                .header("User-Agent", "NetPulse-Android")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyText = response.body?.string() ?: ""
                response.close()

                var ip = ""
                var loc = ""

                bodyText.lines().forEach { line ->
                    if (line.startsWith("ip=")) {
                        ip = line.substringAfter("ip=").trim()
                    } else if (line.startsWith("loc=")) {
                        loc = line.substringAfter("loc=").trim()
                    }
                }

                if (ip.isNotEmpty()) {
                    val countryCode = if (loc.isNotEmpty()) loc else "XX"
                    val countryName = PublicIpInfo.getCountryNameRu(countryCode)
                    val flag = PublicIpInfo.countryCodeToFlagEmoji(countryCode)

                    val info = PublicIpInfo(
                        ip = ip,
                        countryCode = countryCode,
                        countryName = countryName,
                        flagEmoji = flag,
                        isp = "Cloudflare Edge"
                    )
                    cachedInfo = info
                    return@withContext info
                }
            } else {
                response.close()
            }
        } catch (_: Exception) {
        }

        // Strategy 2: ipwho.is or ipapi fallback
        try {
            val request = Request.Builder()
                .url("https://ipwho.is/")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: ""
                response.close()
                val json = JSONObject(jsonStr)
                val ip = json.optString("ip", "")
                val countryCode = json.optString("country_code", "XX")
                val country = json.optString("country", PublicIpInfo.getCountryNameRu(countryCode))
                val connection = json.optJSONObject("connection")
                val isp = connection?.optString("isp", "") ?: ""

                if (ip.isNotEmpty()) {
                    val flag = PublicIpInfo.countryCodeToFlagEmoji(countryCode)
                    val info = PublicIpInfo(
                        ip = ip,
                        countryCode = countryCode,
                        countryName = country,
                        flagEmoji = flag,
                        isp = isp
                    )
                    cachedInfo = info
                    return@withContext info
                }
            } else {
                response.close()
            }
        } catch (_: Exception) {
        }

        return@withContext cachedInfo
    }
}
