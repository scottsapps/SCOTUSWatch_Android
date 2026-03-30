package com.scottsapps.scotuswatch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import org.json.JSONObject

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val CHANNEL_ID = "app_updates"
    private const val NOTIF_ID = 9001
    private const val PREFS_NAME = "scotuswatch_update"
    private const val PREF_NOTIFIED_VERSION = "notified_version"
    const val RELEASES_URL = "https://github.com/scottsapps/SCOTUSWatch_Android/releases/latest"
    private const val API_URL =
        "https://api.github.com/repos/scottsapps/SCOTUSWatch_Android/releases/latest"

    /**
     * Checks GitHub for the latest release on a background thread.
     * If a newer version is found, posts a one-time notification (once per version)
     * and calls [onUpdateAvailable] with the release URL so the caller can show an in-app banner.
     */
    fun check(context: Context, onUpdateAvailable: (releaseUrl: String) -> Unit) {
        Thread {
            try {
                val conn = java.net.URL(API_URL).openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val tagName = JSONObject(body).optString("tag_name", "")
                val latestVersion = tagName.trimStart('v', '.')
                val currentVersion = BuildConfig.VERSION_NAME

                if (latestVersion.isNotEmpty() && isNewer(latestVersion, currentVersion)) {
                    Log.d(TAG, "Update available: $latestVersion (installed: $currentVersion)")

                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    if (prefs.getString(PREF_NOTIFIED_VERSION, "") != latestVersion) {
                        postNotification(context, latestVersion)
                        prefs.edit().putString(PREF_NOTIFIED_VERSION, latestVersion).apply()
                    }

                    onUpdateAvailable(RELEASES_URL)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed", e)
            }
        }.start()
    }

    /** Returns true if [latest] is a higher semver than [current]. */
    private fun isNewer(latest: String, current: String): Boolean {
        fun parts(v: String) = v.split(".").map { it.toIntOrNull() ?: 0 }
        val l = parts(latest)
        val c = parts(current)
        val len = maxOf(l.size, c.size)
        for (i in 0 until len) {
            val diff = l.getOrElse(i) { 0 } - c.getOrElse(i) { 0 }
            if (diff != 0) return diff > 0
        }
        return false
    }

    private fun postNotification(context: Context, version: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifies when a new version of SCOTUSWatch is available" }
        )

        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        manager.notify(
            NOTIF_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("SCOTUSWatch v$version Available")
                .setContentText("Tap to download the latest version from GitHub")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        )
    }
}
