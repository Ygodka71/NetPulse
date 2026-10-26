package com.example.ui.i18n

import com.example.data.preferences.AppLanguage
import com.example.model.MetricHelpInfo
import com.example.model.MetricModuleId
import com.example.model.TestPhase
import java.util.Locale

object AppI18n {

    fun isEn(language: AppLanguage? = null): Boolean {
        if (language == AppLanguage.EN) return true
        if (language == AppLanguage.RU) return false
        val code = Locale.getDefault().language.lowercase()
        return !code.startsWith("ru") && !code.startsWith("be") && !code.startsWith("uk") && !code.startsWith("kk")
    }

    fun getMetricTitle(id: MetricModuleId, isEn: Boolean): String {
        return if (isEn) {
            when (id) {
                MetricModuleId.CURRENT_PING -> "Ping"
                MetricModuleId.AVG_LATENCY -> "Avg Latency"
                MetricModuleId.JITTER -> "Jitter"
                MetricModuleId.PACKET_LOSS -> "Packet Loss"
                MetricModuleId.DOWNLOAD -> "Download"
                MetricModuleId.UPLOAD -> "Upload"
                MetricModuleId.GEO_IP -> "Public IP"
                MetricModuleId.CONNECTION_TYPE -> "Network Type"
            }
        } else {
            id.titleRu
        }
    }

    fun getMetricHelp(id: MetricModuleId, isEn: Boolean): MetricHelpInfo? {
        if (!isEn) {
            return com.example.model.MetricHelpDirectory.getHelpFor(id)
        }
        return when (id) {
            MetricModuleId.JITTER -> MetricHelpInfo(
                title = "Jitter",
                description = "Variation and instability in latency between consecutive data packets. Indicates how erratic response times are.",
                impactText = "Critical for online competitive gaming (causes micro-stutters and teleportation) and voice calls (audio dropouts, robotic speech).",
                goodRange = "• Under 5 ms — Excellent\n• 5–15 ms — Good\n• Over 30 ms — High network instability"
            )
            MetricModuleId.PACKET_LOSS -> MetricHelpInfo(
                title = "Packet Loss",
                description = "Percentage of network packets that failed to reach destination due to buffer overflow, wireless noise, or congestion.",
                impactText = "Even 1–2% loss degrades voice/video call quality and causes noticeable hit registration lag in online gaming.",
                goodRange = "• 0% — Normal\n• 1–3% — Moderate RF interference\n• > 5% — Severe channel degradation"
            )
            MetricModuleId.GEO_IP -> MetricHelpInfo(
                title = "Public IP & Country",
                description = "Your device's outward-facing network address on the global internet, routed through your ISP's Anycast gateways.",
                impactText = "Verifies whether a VPN or proxy is active and which geographical node processes your internet traffic.",
                goodRange = "Should match your physical country and subscribed ISP."
            )
            MetricModuleId.CONNECTION_TYPE -> MetricHelpInfo(
                title = "Connection Type & Link Speed",
                description = "Physical transmission medium (Wi-Fi, 4G LTE, 5G, Ethernet) and raw hardware link speed between your device and router/cell tower.",
                impactText = "Represents physical radio boundary. Real internet throughput cannot exceed this wireless link capacity.",
                goodRange = "Wi-Fi 5 GHz: 300–866+ Mbps\nWi-Fi 2.4 GHz: 72–144 Mbps\n4G LTE: 50–150 Mbps"
            )
            else -> null
        }
    }

    fun getGradeTitle(score: Int, isEn: Boolean): String {
        return if (isEn) {
            when {
                score >= 88 -> "Excellent"
                score >= 70 -> "Good"
                score >= 50 -> "Fair"
                else -> "Poor"
            }
        } else {
            when {
                score >= 88 -> "Отлично"
                score >= 70 -> "Хорошо"
                score >= 50 -> "Удовлетворительно"
                else -> "Плохо"
            }
        }
    }

    fun getGradeSubtitle(score: Int, isEn: Boolean): String {
        return if (isEn) {
            when {
                score >= 88 -> "Ideal for competitive gaming and 4K streaming"
                score >= 70 -> "Great for video calls and HD streaming"
                score >= 50 -> "Possible latency spikes in real-time gaming"
                else -> "High latency or packet loss detected"
            }
        } else {
            when {
                score >= 88 -> "Идеально для онлайн-игр и 4K стриминга"
                score >= 70 -> "Подходит для видеозвонков и HD видео"
                score >= 50 -> "Возможны задержки в играх"
                else -> "Высокая задержка или потеря пакетов"
            }
        }
    }

    fun getPhaseLabel(phase: TestPhase, isEn: Boolean): String {
        return if (isEn) {
            when (phase) {
                TestPhase.IDLE -> "READY TO TEST"
                TestPhase.PINGING -> "CALIBRATING PING"
                TestPhase.DOWNLOADING -> "DOWNLOADING"
                TestPhase.UPLOADING -> "UPLOADING"
                TestPhase.COMPLETED -> "RESULT"
            }
        } else {
            when (phase) {
                TestPhase.IDLE -> "ГОТОВ К ТЕСТУ"
                TestPhase.PINGING -> "ПРОВЕРКА PING"
                TestPhase.DOWNLOADING -> "СКАЧИВАНИЕ"
                TestPhase.UPLOADING -> "ОТДАЧА"
                TestPhase.COMPLETED -> "РЕЗУЛЬТАТ"
            }
        }
    }

    fun getPhaseStatusText(phase: TestPhase, isEn: Boolean): String {
        return if (isEn) {
            when (phase) {
                TestPhase.IDLE -> ""
                TestPhase.PINGING -> "Checking latency and jitter…"
                TestPhase.DOWNLOADING -> "Testing download speed…"
                TestPhase.UPLOADING -> "Testing upload speed…"
                TestPhase.COMPLETED -> "Testing completed"
            }
        } else {
            when (phase) {
                TestPhase.IDLE -> ""
                TestPhase.PINGING -> "Проверка задержки и джиттера…"
                TestPhase.DOWNLOADING -> "Тестирование скорости загрузки…"
                TestPhase.UPLOADING -> "Тестирование скорости отдачи…"
                TestPhase.COMPLETED -> "Тестирование завершено"
            }
        }
    }
}
