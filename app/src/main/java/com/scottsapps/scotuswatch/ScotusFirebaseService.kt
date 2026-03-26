package com.scottsapps.scotuswatch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ScotusFirebaseService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "ScotusFirebase"
        const val CHANNEL_ID = "scotus_alerts"

        /**
         * Register an FCM token with the Lambda backend.
         * Exposed as a companion-object function so MainActivity can call it
         * without instantiating the service directly.
         */
        fun registerTokenWithBackend(token: String) {
            Thread {
                try {
                    val url = java.net.URL("${BuildConfig.API_BASE_URL}/fcm-token")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "PUT"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("x-api-key", BuildConfig.API_KEY)
                    conn.doOutput = true
                    conn.outputStream.write("""{"token":"$token"}""".toByteArray())
                    val status = conn.responseCode
                    Log.d(TAG, "Token registration: HTTP $status")
                    conn.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Token registration failed", e)
                }
            }.start()
        }
    }

    /**
     * Called when a new FCM token is generated (first launch or token refresh).
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: ${token.take(8)}...")
        registerTokenWithBackend(token)
    }

    /**
     * Called when a data message is received.
     * We always build a local notification so the user sees it regardless of
     * whether the app is in the foreground or background.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val alertBody = message.data["alertBody"]
            ?: message.notification?.body
            ?: return

        val documentUrl = message.data["fileURL"]
        showNotification(alertBody, documentUrl)
    }

    private fun showNotification(body: String, url: String?) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (required on Android 8+, no-op if already exists)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SCOTUS Document Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when new Supreme Court documents are posted"
        }
        manager.createNotificationChannel(channel)

        // Tapping the notification opens the document URL in the browser
        val intent = if (url != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        } else {
            packageManager.getLaunchIntentForPackage(packageName)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("SCOTUSWatch")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Use body hashCode so multiple notifications don't overwrite each other
        manager.notify(body.hashCode(), notification)
    }
}
