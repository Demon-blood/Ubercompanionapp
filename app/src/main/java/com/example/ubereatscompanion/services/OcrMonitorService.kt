package com.example.ubereatscompanion.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.media.projection.MediaProjection.Callback
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.ubereatscompanion.R
import com.example.ubereatscompanion.UberCompanionApp
import com.example.ubereatscompanion.engine.OfferDecisionEngine
import com.example.ubereatscompanion.features.TaskerBridge
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class OcrMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val parser = OfferTextParser
    private val engine = OfferDecisionEngine()

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var handlerThread: HandlerThread? = null
    private var lastProcessedAt = 0L
    private var projectionCallback: Callback? = null
    private val processing = AtomicBoolean(false)
    private val stopping = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(
            102,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Uber Eats Companion OCR")
                .setContentText("Screen text reader is active when permission is granted")
                .setOngoing(true)
                .build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION") intent?.getParcelableExtra(EXTRA_DATA)
        }

        if (resultCode == 0 || data == null) {
            Log.w(TAG, "OCR permission data missing; service started but cannot capture screen.")
            return START_NOT_STICKY
        }

        startCapture(resultCode, data)
        return START_STICKY
    }

    override fun onDestroy() {
        stopCapture()
        recognizer.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startCapture(resultCode: Int, data: Intent) {
        if (mediaProjection != null) return

        val metrics = DisplayMetrics()
        val windowManager = getSystemService(WindowManager::class.java)
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        handlerThread = HandlerThread("OcrCaptureThread").also { it.start() }
        val handler = Handler(handlerThread!!.looper)

        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        projectionCallback = object : Callback() {
            override fun onStop() { stopCapture() }
        }
        mediaProjection?.registerCallback(projectionCallback!!, handler)

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        imageReader?.setOnImageAvailableListener({ reader ->
            val now = System.currentTimeMillis()
            if (now - lastProcessedAt < OCR_INTERVAL_MS || !processing.compareAndSet(false, true)) {
                reader.acquireLatestImage()?.close()
                return@setOnImageAvailableListener
            }
            lastProcessedAt = now
            val image = reader.acquireLatestImage()
            if (image == null) {
                processing.set(false)
                return@setOnImageAvailableListener
            }
            processImage(image)
        }, handler)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "UberEatsCompanionOCR",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            handler
        )
    }

    private fun processImage(image: Image) {
        scope.launch {
            try {
                val bitmap = image.toBitmap().let { original ->
                    val scaledWidth = (original.width * 0.5f).toInt().coerceAtLeast(1)
                    val scaledHeight = (original.height * 0.5f).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(original, scaledWidth, scaledHeight, true).also {
                        if (it !== original) original.recycle()
                    }
                }
                val input = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(input)
                    .addOnSuccessListener { result -> handleRecognizedText(result.text) }
                    .addOnFailureListener { Log.e(TAG, "OCR recognition failed", it) }
                    .addOnCompleteListener {
                        bitmap.recycle()
                        processing.set(false)
                    }
            } catch (t: Throwable) {
                Log.e(TAG, "OCR frame processing failed", t)
                processing.set(false)
            } finally {
                image.close()
            }
        }
    }

    private fun handleRecognizedText(text: String) {
        val offer = parser.parse(text, currentBatteryPercent = 100.0) ?: return
        val location = AppState.location.value
        val enriched = offer.copy(currentLat = location?.latitude, currentLng = location?.longitude)
        scope.launch {
            runCatching {
                val repository = (application as UberCompanionApp).repository
                val ruleEntity = repository.currentRuleEntity()
                val settings = repository.currentSettings()
                val decision = engine.evaluate(enriched, settings, AppState.weather.value, repository.findStoreRule(enriched.pickupName))
                AppState.updateOffer(enriched, decision)
                repository.saveEvaluatedOffer(enriched, decision)
                if (ruleEntity.taskerEnabled && ruleEntity.taskerBroadcastEvents) {
                    TaskerBridge.publishOfferEvent(
                        context = this@OcrMonitorService,
                        source = "OCR",
                        offer = enriched,
                        decision = decision,
                        runNamedTask = ruleEntity.taskerRunNamedTasks,
                        acceptTaskName = ruleEntity.taskerTaskOnAccept,
                        maybeTaskName = ruleEntity.taskerTaskOnMaybe,
                        declineTaskName = ruleEntity.taskerTaskOnDecline,
                        showConfirmation = ruleEntity.requireUserConfirmation
                    )
                }
            }.onFailure { Log.e(TAG, "Failed to save OCR offer", it) }
        }
    }

    private fun Image.toBitmap(): Bitmap {
        val plane = planes[0]
        val buffer: ByteBuffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        return Bitmap.createBitmap(bitmap, 0, 0, width, height).also { bitmap.recycle() }
    }

    private fun stopCapture() {
        if (!stopping.compareAndSet(false, true)) return
        imageReader?.setOnImageAvailableListener(null, null)
        virtualDisplay?.release()
        imageReader?.close()
        projectionCallback?.let { callback -> mediaProjection?.unregisterCallback(callback) }
        mediaProjection?.stop()
        handlerThread?.quitSafely()
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
        handlerThread = null
        projectionCallback = null
        stopping.set(false)
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "OCR fallback", NotificationManager.IMPORTANCE_LOW))
    }

    companion object {
        private const val TAG = "OcrMonitorService"
        private const val CHANNEL_ID = "ocr_fallback"
        private const val OCR_INTERVAL_MS = 1_500L
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
    }
}
