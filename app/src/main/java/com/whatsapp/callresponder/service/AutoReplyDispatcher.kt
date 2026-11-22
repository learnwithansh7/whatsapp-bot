package com.whatsapp.callresponder.service

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log

object AutoReplyDispatcher {
    private const val TAG = "AutoReplyDispatcher"

    /**
     * Attempts to send a reply using the notification's RemoteInput action,
     * or fallback to WhatsApp direct chat intent.
     */
    fun dispatchReply(
        context: Context,
        sbn: StatusBarNotification,
        replyText: String,
        callerNumber: String?
    ): Boolean {
        // 1. Try finding a RemoteInput on the notification actions
        val notification = sbn.notification
        val actions = notification.actions ?: arrayOf()

        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (remoteInput in remoteInputs) {
                if (action.actionIntent != null) {
                    try {
                        val intent = Intent()
                        val bundle = Bundle()
                        bundle.putCharSequence(remoteInput.resultKey, replyText)
                        RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)
                        action.actionIntent.send(context, 0, intent)
                        Log.i(TAG, "Successfully dispatched reply via RemoteInput to ${sbn.packageName}")
                        return true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error executing RemoteInput pending intent", e)
                    }
                }
            }
        }

        // 2. Fallback: If caller number is available, open direct send intent
        if (!callerNumber.isNullOrBlank()) {
            try {
                val cleanNumber = callerNumber.replace(Regex("[^0-9+]"), "")
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(replyText)}")
                val sendIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    setPackage(sbn.packageName)
                }
                context.startActivity(sendIntent)
                Log.i(TAG, "Dispatched via WhatsApp direct chat intent for $cleanNumber")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error executing fallback WhatsApp intent", e)
            }
        }

        Log.w(TAG, "Could not find a valid RemoteInput or phone number to send reply")
        return false
    }
}
