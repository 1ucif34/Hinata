package com.shazeb.hinata.commands

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationListener : NotificationListenerService() {

    companion object {
        var lastNotifications = mutableListOf<String>()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let {
            val appName = it.packageName
            val title = it.notification.extras
                .getString("android.title") ?: ""
            val text = it.notification.extras
                .getString("android.text") ?: ""

            if (title.isNotEmpty()) {
                val entry = "From $appName: $title — $text"
                lastNotifications.add(0, entry)

                // Keep only last 20 notifications
                if (lastNotifications.size > 20) {
                    lastNotifications.removeAt(lastNotifications.size - 1)
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Notification dismissed
    }
}
