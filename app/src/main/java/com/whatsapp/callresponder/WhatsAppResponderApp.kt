package com.whatsapp.callresponder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.whatsapp.callresponder.data.local.AppDatabase

class WhatsAppResponderApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto-Reply Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors WhatsApp missed calls and dispatches auto-replies"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "whatsapp_call_responder_channel"
    }
}
