package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.model.MetricModuleId
import com.example.model.MetricModuleItem
import com.example.model.PingTarget

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage {
    SYSTEM,
    RU,
    EN
}

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("netpulse_user_prefs", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() {
            val raw = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
            return try {
                ThemeMode.valueOf(raw)
            } catch (_: Exception) {
                ThemeMode.SYSTEM
            }
        }
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    var appLanguage: AppLanguage
        get() {
            val raw = prefs.getString(KEY_APP_LANGUAGE, AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name
            return try {
                AppLanguage.valueOf(raw)
            } catch (_: Exception) {
                AppLanguage.SYSTEM
            }
        }
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value.name).apply()

    var notificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_ENABLED, value).apply()

    var notifShowPing: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_PING, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_PING, value).apply()

    var notifShowJitter: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_JITTER, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_JITTER, value).apply()

    var notifShowIp: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_IP, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_IP, value).apply()

    var notifShowSpeed: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_SPEED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_SPEED, value).apply()

    var selectedServerId: String
        get() = prefs.getString(KEY_SERVER_ID, "cf") ?: "cf"
        set(value) = prefs.edit().putString(KEY_SERVER_ID, value).apply()

    fun getModules(): List<MetricModuleItem> {
        val raw = prefs.getString(KEY_MODULES, null)
        if (raw.isNullOrBlank()) {
            return defaultModules()
        }

        return try {
            raw.split(";").mapNotNull { part ->
                val tokens = part.split(":")
                val idStr = tokens.getOrNull(0) ?: return@mapNotNull null
                val isVisible = tokens.getOrNull(1)?.toBooleanStrictOrNull() ?: true
                val id = try { MetricModuleId.valueOf(idStr) } catch (_: Exception) { null }
                id?.let { MetricModuleItem(it, isVisible) }
            }.ifEmpty { defaultModules() }
        } catch (_: Exception) {
            defaultModules()
        }
    }

    fun saveModules(modules: List<MetricModuleItem>) {
        val encoded = modules.joinToString(";") { "${it.id.name}:${it.isVisible}" }
        prefs.edit().putString(KEY_MODULES, encoded).apply()
    }

    fun defaultModules(): List<MetricModuleItem> {
        return listOf(
            MetricModuleItem(MetricModuleId.CONNECTION_TYPE, isVisible = true),
            MetricModuleItem(MetricModuleId.GEO_IP, isVisible = true),
            MetricModuleItem(MetricModuleId.CURRENT_PING, isVisible = true),
            MetricModuleItem(MetricModuleId.AVG_LATENCY, isVisible = true),
            MetricModuleItem(MetricModuleId.JITTER, isVisible = true),
            MetricModuleItem(MetricModuleId.PACKET_LOSS, isVisible = true),
            MetricModuleItem(MetricModuleId.DOWNLOAD, isVisible = true),
            MetricModuleItem(MetricModuleId.UPLOAD, isVisible = true)
        )
    }

    fun getCustomServers(): List<PingTarget> {
        val raw = prefs.getString(KEY_CUSTOM_SERVERS, null) ?: return emptyList()
        return try {
            raw.split(";").mapNotNull { entry ->
                val tokens = entry.split("|")
                if (tokens.size >= 4) {
                    val id = tokens[0]
                    val name = tokens[1]
                    val host = tokens[2]
                    val port = tokens[3].toIntOrNull() ?: 53
                    val fallback = if (tokens.size >= 5) tokens[4] else "https://$host"
                    PingTarget(
                        id = id,
                        name = name,
                        host = host,
                        port = port,
                        httpFallbackUrl = fallback,
                        isCustom = true
                    )
                } else null
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCustomServers(servers: List<PingTarget>) {
        val encoded = servers.filter { it.isCustom }.joinToString(";") {
            "${it.id}|${it.name}|${it.host}|${it.port}|${it.httpFallbackUrl}"
        }
        prefs.edit().putString(KEY_CUSTOM_SERVERS, encoded).apply()
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_APP_LANGUAGE = "key_app_language"
        private const val KEY_NOTIF_ENABLED = "key_notif_enabled"
        private const val KEY_NOTIF_PING = "key_notif_ping"
        private const val KEY_NOTIF_JITTER = "key_notif_jitter"
        private const val KEY_NOTIF_IP = "key_notif_ip"
        private const val KEY_NOTIF_SPEED = "key_notif_speed"
        private const val KEY_SERVER_ID = "key_server_id"
        private const val KEY_MODULES = "key_modules"
        private const val KEY_CUSTOM_SERVERS = "key_custom_servers"
    }
}
