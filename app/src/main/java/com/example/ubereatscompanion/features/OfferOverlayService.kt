package com.example.ubereatscompanion.features

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.ubereatscompanion.R
import com.example.ubereatscompanion.model.OfferDecision
import com.example.ubereatscompanion.model.Recommendation

class OfferOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(43, notification())
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun showDecision(decision: OfferDecision) {
        if (!Settings.canDrawOverlays(this)) return
        val view = overlayView ?: TextView(this).also {
            it.textSize = 16f
            it.setPadding(28, 18, 28, 18)
            it.setTextColor(android.graphics.Color.WHITE)
            overlayView = it
            windowManager?.addView(it, params())
        }
        val title = when (decision.recommendation) {
            Recommendation.ACCEPT -> "ACCEPT"
            Recommendation.MAYBE -> "MAYBE"
            Recommendation.DECLINE -> "DECLINE"
        }
        view.setBackgroundColor(
            when (decision.recommendation) {
                Recommendation.ACCEPT -> android.graphics.Color.rgb(26, 135, 84)
                Recommendation.MAYBE -> android.graphics.Color.rgb(196, 131, 0)
                Recommendation.DECLINE -> android.graphics.Color.rgb(180, 45, 45)
            }
        )
        view.text = "$title  ${decision.score.toInt()}/100\n${decision.euroPerKm?.let { "€%.2f/km".format(it) } ?: "€/km ?"}  ${decision.euroPerHour?.let { "€%.0f/h".format(it) } ?: "€/h ?"}\n${decision.reasons.firstOrNull().orEmpty()}"
    }

    override fun onDestroy() {
        overlayView?.let { runCatching { windowManager?.removeView(it) } }
        overlayView = null
        super.onDestroy()
    }

    private fun params(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 120
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("overlay", "Offer overlay", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun notification(): Notification = NotificationCompat.Builder(this, "overlay")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Uber Eats Companion overlay")
        .setContentText("Showing live offer recommendations")
        .setOngoing(true)
        .build()
}
