package com.deliverycompanion.aio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1001, buildNotification())
        showOverlay()
    }

    override fun onDestroy() {
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        super.onDestroy()
    }

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = LayoutInflater.from(this).inflate(R.layout.overlay_panel, null)

        val decisionText = view.findViewById<TextView>(R.id.decisionText)
        val payInput = view.findViewById<EditText>(R.id.payInput)
        val kmInput = view.findViewById<EditText>(R.id.kmInput)
        val minInput = view.findViewById<EditText>(R.id.minInput)
        val evalButton = view.findViewById<Button>(R.id.evaluateButton)
        val closeButton = view.findViewById<Button>(R.id.closeButton)

        val rules = LocalStore.loadRules(this)

        evalButton.setOnClickListener {
            val offer = OfferInput(
                estimatedPay = payInput.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0,
                estimatedDistanceKm = kmInput.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0,
                estimatedMinutes = minInput.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            )

            val result = OfferEvaluator.evaluate(offer, rules)
            decisionText.text = "${result.recommendation} · Score ${result.score}/100\n${money(result.payPerKm)}/km · ${money(result.hourlyRate)}/hour"

            if (result.recommendation == "ACCEPT") {
                decisionText.setBackgroundResource(R.drawable.decision_accept_bg)
            } else {
                decisionText.setBackgroundResource(R.drawable.decision_reject_bg)
            }
        }

        closeButton.setOnClickListener { stopSelf() }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 32
            y = 160
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }

        overlayView = view
        windowManager.addView(view, params)
    }

    private fun buildNotification(): Notification {
        val channelId = "delivery_companion_overlay"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Delivery Companion Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("Delivery Companion overlay running")
                .setContentText("Manual offer decision helper is active.")
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Delivery Companion overlay running")
                .setContentText("Manual offer decision helper is active.")
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .build()
        }
    }
}