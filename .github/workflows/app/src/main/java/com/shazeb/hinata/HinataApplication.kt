package com.shazeb.hinata

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class HinataApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "hinata_voice_channel",
            "Hinata AI Voice Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps Hinata AI listening for your voice"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
