package com.example.ubereatscompanion.features

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.ubereatscompanion.R

object ConfirmationLauncher {
    private const val CHANNEL_ID = "offer_confirmation"
    private const val NOTIFICATION_ID = 4207

    fun showConfirmation(context: Context) {
        val activityIntent = Intent(context, OfferConfirmationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        runCatching { context.startActivity(activityIntent) }
            .onFailure { showNotification(context, activityIntent) }
    }

    fun showNotification(context: Context, activityIntent: Intent? = null) {
        ensureChannel(context)
        val intent = activityIntent ?: Intent(context, OfferConfirmationActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            4207,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Uber Eats Companion")
            .setContentText("Tap to confirm the recommended offer decision.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Offer confirmations",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Full-screen confirmations for detected delivery offers."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
