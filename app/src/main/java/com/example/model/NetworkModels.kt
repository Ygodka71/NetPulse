package com.example.model

enum class ConnectionType {
    WIFI,
    CELLULAR,
    ETHERNET,
    NONE
}

data class NetworkInfoState(
    val connectionType: ConnectionType = ConnectionType.NONE,
    val isConnected: Boolean = false,
    val isInternetValidated: Boolean = false,
    val ipAddress: String = "—",
    val linkDownstreamSpeedMbps: Int = 0,
    val networkName: String = "Нет подключения"
) {
    fun getDisplayName(isEn: Boolean): String {
        return when (connectionType) {
            ConnectionType.WIFI -> if (isEn) "Wi-Fi" else "Wi-Fi сеть"
            ConnectionType.CELLULAR -> if (isEn) "Cellular" else "Мобильная сеть"
            ConnectionType.ETHERNET -> "Ethernet"
            ConnectionType.NONE -> if (isEn) "Offline" else "Не подключено"
        }
    }
}

data class PingTarget(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 53,
    val httpFallbackUrl: String = "https://1.1.1.1/cdn-cgi/trace",
    val isCustom: Boolean = false
) {
    companion object {
        val CLOUDFLARE = PingTarget("cf", "Cloudflare Edge", "1.1.1.1", 53, "https://1.1.1.1/cdn-cgi/trace", isCustom = false)
        val GOOGLE = PingTarget("google", "Google DNS", "8.8.8.8", 53, "https://www.google.com/generate_204", isCustom = false)
        val QUAD9 = PingTarget("quad9", "Quad9 Secure", "9.9.9.9", 53, "https://dns.quad9.net/dns-query", isCustom = false)

        val DEFAULT_TARGETS = listOf(CLOUDFLARE, GOOGLE, QUAD9)
    }
}

data class PingStatistics(
    val currentPingMs: Long = 0,
    val avgLatencyMs: Long = 0,
    val minLatencyMs: Long = 0,
    val maxLatencyMs: Long = 0,
    val jitterMs: Long = 0,
    val packetLossPercent: Int = 0,
    val historySamples: List<Float> = emptyList(),
    val totalPingsSent: Int = 0
)

enum class TestPhase {
    IDLE,
    PINGING,
    DOWNLOADING,
    UPLOADING,
    COMPLETED
}

data class SpeedMeasurementResult(
    val finalAvgMbps: Double = 0.0,
    val peakMbps: Double = 0.0,
    val minMbps: Double = 0.0
)

data class SpeedState(
    val phase: TestPhase = TestPhase.IDLE,
    val currentMbps: Double = 0.0,
    val peakMbps: Double = 0.0,
    val downloadResult: SpeedMeasurementResult = SpeedMeasurementResult(),
    val uploadResult: SpeedMeasurementResult = SpeedMeasurementResult(),
    val downloadFinalMbps: Double = 0.0,
    val uploadFinalMbps: Double = 0.0,
    val progressPercent: Float = 0f,
    val statusText: String = "",
    val completedTimestamp: Long = 0L
)

enum class GradeType {
    EXCELLENT,
    GREAT,
    GOOD,
    BASIC,
    UNSTABLE
}

data class NetworkQualityGrade(
    val title: String,
    val subtitle: String,
    val scoreOutOf100: Int,
    val colorHex: Long,
    val gradeType: GradeType = GradeType.EXCELLENT
)

