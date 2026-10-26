package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.preferences.AppPreferences

class StopMonitorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = AppPreferences(context)
        prefs.notificationEnabled = false
        LiveNotificationManager(context).cancelNotification()
        NetPulseLiveService.stop(context)
    }
}
