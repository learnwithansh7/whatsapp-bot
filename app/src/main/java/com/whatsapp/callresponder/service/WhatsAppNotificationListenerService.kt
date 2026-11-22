package com.whatsapp.callresponder.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class WhatsAppNotificationListenerService : NotificationListenerService() {

    private lateinit var humanSimulationEngine: HumanSimulationEngine

    override fun onCreate() {
        super.onCreate()
        humanSimulationEngine = HumanSimulationEngine(this)
        Log.i(TAG, "WhatsAppNotificationListenerService started and listening.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        if (packageName != WHATSAPP_PKG && packageName != WHATSAPP_BUSINESS_PKG) {
            return
        }

        val extras = sbn.notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: ""
        val subText = extras.getString(Notification.EXTRA_SUB_TEXT) ?: ""

        val combinedContent = "$text $subText".lowercase()

        // Detect if this is a missed call notification
        val isMissedCall = combinedContent.contains("missed") &&
                (combinedContent.contains("call") || combinedContent.contains("video"))

        if (isMissedCall && title.isNotBlank()) {
            val callerName = title.trim()
            Log.i(TAG, "Detected WhatsApp missed call from: $callerName (Text: $text)")

            // Extract phone number if title looks like phone number (+91...)
            val callerNumber = if (callerName.matches(Regex("^[+]?[0-9\\s-]{7,18}$"))) {
                callerName.replace(Regex("[^0-9+]"), "")
            } else {
                null
            }

            humanSimulationEngine.processMissedCall(
                sbn = sbn,
                callerName = callerName,
                callerNumber = callerNumber
            )
        } else {
            // If the user sends a manual message to this contact, cancel pending reply
            if (extras.getBoolean(Notification.EXTRA_SHOW_WHEN, false) && title.isNotBlank()) {
                // Potential user active conversation
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        if (packageName == WHATSAPP_PKG || packageName == WHATSAPP_BUSINESS_PKG) {
            val title = sbn.notification.extras?.getString(Notification.EXTRA_TITLE) ?: return
            // If notification dismissed, we can optionally cancel or allow it
        }
    }

    companion object {
        private const val TAG = "WANotificationListener"
        const val WHATSAPP_PKG = "com.whatsapp"
        const val WHATSAPP_BUSINESS_PKG = "com.whatsapp.w4b"
    }
}
