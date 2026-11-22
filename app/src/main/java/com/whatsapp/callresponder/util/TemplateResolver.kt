package com.whatsapp.callresponder.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TemplateResolver {
    fun resolve(template: String, callerName: String, timestamp: Long = System.currentTimeMillis()): String {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(Date(timestamp))

        return template
            .replace("{name}", callerName.trim())
            .replace("{time}", formattedTime)
    }
}
