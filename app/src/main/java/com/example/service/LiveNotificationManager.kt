package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.ThemeMode
import com.example.model.NetworkInfoState
import com.example.model.PingStatistics
import com.example.model.PublicIpInfo
import com.example.model.SpeedState

class LiveNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val prefs = AppPreferences(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NetPulse — Пинг и статус сети (Live)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Отображает текущий пинг в строке состояния (Now Bar) и подробности в шторке"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(
        pingStats: PingStatistics,
        networkInfo: NetworkInfoState,
        speedState: SpeedState,
        publicIp: PublicIpInfo?
    ): Notification {
        // Tap to open app
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action button to stop/pause live monitor
        val stopIntent = Intent(context, StopMonitorReceiver::class.java)
        val pendingStopIntent = PendingIntent.getBroadcast(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isEn = com.example.ui.i18n.AppI18n.isEn()
        val unitMs = if (isEn) "ms" else "мс"
        val pingStr = if (pingStats.currentPingMs > 0) "${pingStats.currentPingMs} $unitMs" else "—"

        // High-contrast color resolution: prevent OEM skins (MIUI/HyperOS/OneUI) from inverting text to black
        val sysNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val isNight = sysNight || prefs.themeMode == ThemeMode.DARK

        val primaryTextColor = if (isNight) Color.WHITE else Color.parseColor("#0F172A")
        val secondaryTextColor = if (isNight) Color.parseColor("#CBD5E1") else Color.parseColor("#475569")
        val accentColor = if (isNight) Color.parseColor("#38BDF8") else Color.parseColor("#0284C7")

        fun span(text: CharSequence, color: Int, isBold: Boolean = false): SpannableString {
            return SpannableString(text).apply {
                setSpan(ForegroundColorSpan(color), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (isBold) {
                    setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }

        val titleText = if (isEn) "Ping" else "Пинг"
        val titleSpan = span(titleText, primaryTextColor, isBold = true)
        val contentTextSpan = span(pingStr, accentColor, isBold = true)

        val bigTextBuilder = SpannableStringBuilder()

        fun appendLine(iconAndLabel: String, value: String, extra: String = "") {
            if (bigTextBuilder.isNotEmpty()) bigTextBuilder.append("\n")
            bigTextBuilder.append(span(iconAndLabel, primaryTextColor))
            bigTextBuilder.append(span(value, accentColor, isBold = true))
            if (extra.isNotEmpty()) {
                bigTextBuilder.append(span(extra, secondaryTextColor))
            }
        }

        val labelPing = if (isEn) "⚡ Ping: " else "⚡ Пинг: "
        val avgExtra = if (isEn) " (avg ${pingStats.avgLatencyMs} ms)" else " (ср. ${pingStats.avgLatencyMs} мс)"
        appendLine(labelPing, pingStr, avgExtra)

        if (prefs.notifShowJitter) {
            val jitterStr = if (pingStats.jitterMs > 0) "${pingStats.jitterMs} $unitMs" else "0 $unitMs"
            val labelJitter = if (isEn) "📶 Jitter: " else "📶 Джиттер: "
            val labelLoss = if (isEn) "  |  Loss: " else "  |  Потери: "
            appendLine(labelJitter, jitterStr, "$labelLoss${pingStats.packetLossPercent}%")
        }

        if (prefs.notifShowSpeed && speedState.currentMbps > 0) {
            val speedFormatted = String.format(java.util.Locale.US, "%.1f", speedState.currentMbps)
            val labelSpeed = if (isEn) "🚀 Speed: " else "🚀 Скорость: "
            val unitSpeed = if (isEn) "Mbps" else "Мбит/с"
            appendLine(labelSpeed, "$speedFormatted $unitSpeed")
        }

        if (prefs.notifShowIp && publicIp != null) {
            appendLine("🌐 IP: ", publicIp.ip, " ${publicIp.flagEmoji} (${publicIp.getCountryDisplayName(isEn)})")
        }

        val labelNet = if (isEn) "🔗 Network: " else "🔗 Сеть: "
        appendLine(labelNet, networkInfo.getDisplayName(isEn))

        val bigTitlePrefix = if (isEn) "NetPulse • Ping " else "NetPulse • Пинг "
        val bigTitleBuilder = SpannableStringBuilder()
        bigTitleBuilder.append(span(bigTitlePrefix, primaryTextColor, isBold = true))
        bigTitleBuilder.append(span(pingStr, accentColor, isBold = true))

        // Vibrant blue accent matching user request
        val pillBlueColor = 0xFF2563EB.toInt()
        val stopActionTitle = if (isEn) "Stop" else "Остановить"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_status_pulse)
            .setContentTitle(titleSpan)
            .setContentText(contentTextSpan)
            .setTicker("$titleText $pingStr")
            .setColor(pillBlueColor)
            .setColorized(false)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(bigTitleBuilder)
                    .bigText(bigTextBuilder)
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(pendingOpenIntent)
            .addAction(R.drawable.ic_status_pulse, stopActionTitle, pendingStopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Promoted ongoing live activity flags for One UI and Android 16
        builder.extras.putBoolean("android.requestPromotedOngoing", true)
        builder.extras.putBoolean("com.samsung.android.notification.pill", true)
        builder.extras.putString("android.substName", "NetPulse")

        return builder.build()
    }

    fun updateNotification(
        pingStats: PingStatistics,
        networkInfo: NetworkInfoState,
        speedState: SpeedState,
        publicIp: PublicIpInfo?
    ) {
        if (!prefs.notificationEnabled) {
            cancelNotification()
            return
        }

        NetPulseLiveService.latestPingStats = pingStats
        NetPulseLiveService.latestNetworkInfo = networkInfo
        NetPulseLiveService.latestSpeedState = speedState
        NetPulseLiveService.latestPublicIp = publicIp

        val notification = buildNotification(pingStats, networkInfo, speedState, publicIp)

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun cancelNotification() {
        try {
            notificationManager.cancel(NOTIFICATION_ID)
            NetPulseLiveService.stop(context)
        } catch (_: Exception) {
        }
    }

    companion object {
        const val CHANNEL_ID = "netpulse_live_channel"
        const val NOTIFICATION_ID = 4041
    }
}
