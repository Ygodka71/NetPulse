package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.example.data.preferences.AppPreferences
import com.example.model.NetworkInfoState
import com.example.model.PingStatistics
import com.example.model.PublicIpInfo
import com.example.model.SpeedState

class NetPulseLiveService : Service() {

    private lateinit var notificationManager: LiveNotificationManager
    private lateinit var prefs: AppPreferences

    override fun onCreate() {
        super.onCreate()
        notificationManager = LiveNotificationManager(this)
        prefs = AppPreferences(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!prefs.notificationEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Build initial notification
        val notification = notificationManager.buildNotification(
            pingStats = latestPingStats,
            networkInfo = latestNetworkInfo,
            speedState = latestSpeedState,
            publicIp = latestPublicIp
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        LiveNotificationManager.NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(
                        LiveNotificationManager.NOTIFICATION_ID,
                        notification
                    )
                }
            } else {
                startForeground(
                    LiveNotificationManager.NOTIFICATION_ID,
                    notification
                )
            }
        } catch (_: Exception) {
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    companion object {
        var latestPingStats: PingStatistics = PingStatistics()
        var latestNetworkInfo: NetworkInfoState = NetworkInfoState()
        var latestSpeedState: SpeedState = SpeedState()
        var latestPublicIp: PublicIpInfo? = null

        fun start(context: Context) {
            try {
                val intent = Intent(context, NetPulseLiveService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, NetPulseLiveService::class.java)
                context.stopService(intent)
            } catch (_: Exception) {
            }
        }
    }
}
