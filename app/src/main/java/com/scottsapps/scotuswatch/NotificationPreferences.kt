package com.scottsapps.scotuswatch

import android.content.Context
import android.content.SharedPreferences

/**
 * Thin wrapper around SharedPreferences for per-document-type notification
 * preferences. Keys mirror the docTypeKey values sent in the FCM payload so
 * the Firebase service can look them up without any translation.
 */
object NotificationPreferences {

    private const val PREFS_NAME = "notification_prefs"

    // All types default to enabled.
    private val DEFAULTS = mapOf(
        "slip_opinions"      to true,
        "in_chambers"        to true,
        "relating_to_orders" to true,
        "orders"             to true,
        "misc_order"         to true,
    )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context, docTypeKey: String): Boolean =
        prefs(context).getBoolean(docTypeKey, DEFAULTS[docTypeKey] ?: true)

    fun setEnabled(context: Context, docTypeKey: String, enabled: Boolean) {
        prefs(context).edit().putBoolean(docTypeKey, enabled).apply()
    }

    /** Returns the current enabled state for every known document type. */
    fun getAll(context: Context): Map<String, Boolean> =
        DEFAULTS.keys.associateWith { isEnabled(context, it) }
}
