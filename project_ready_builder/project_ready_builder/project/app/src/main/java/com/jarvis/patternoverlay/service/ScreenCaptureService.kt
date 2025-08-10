package com.jarvis.patternoverlay.service

import android.app.*
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
import android.os.Binder
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.jarvis.patternoverlay.R
import com.jarvis.patternoverlay.ui.MainActivity
import kotlinx.coroutines.*
import timber.log.Timber
import java.nio.ByteBuffer

class ScreenCaptureService : Service() {
    
    private val binder = LocalBinder()
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var isCapturing = false
    private var captureListener: ((Bitmap) -> Unit)? = null
    
    // Capture settings
    private var fps = 2 // Configurable FPS
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screen_capture_channel"
        
        const val ACTION_START_CAPTURE = "START_CAPTURE"
        const val ACTION_STOP_CAPTURE = "STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "RESULT_CODE"
        const val EXTRA_DATA = "DATA"
    }
    
    inner class LocalBinder : Binder() {
        fun getService(): ScreenCaptureService = this@ScreenCaptureService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        getScreenMetrics()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CAPTURE -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
                if (data != null) {
                    startCapture(resultCode, data)
                }
            }
            ACTION_STOP_CAPTURE -> {
                stopCapture()
            }
        }
        return START_STICKY
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen Capture Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Captures screen for pattern analysis"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun getScreenMetrics() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        screenDensity = displayMetrics.densityDpi
    }
    
    fun startCapture(resultCode: Int, data: Intent) {
        if (isCapturing) return
        
        try {
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            
            setupImageReader()
            setupVirtualDisplay()
            
            isCapturing = true
            
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            
            startCaptureLoop()
            
            Timber.d("Screen capture started successfully")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to start screen capture")
            stopCapture()
        }
    }
    
    private fun setupImageReader() {
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader?.acquireLatestImage()
            if (image != null) {
                processCapturedImage(image)
                image.close()
            }
        }, null)
    }
    
    private fun setupVirtualDisplay() {
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }
    
    private fun startCaptureLoop() {
        serviceScope.launch {
            while (isCapturing) {
                delay(1000L / fps) // Control capture rate
                // ImageReader will handle the actual capture via callback
            }
        }
    }
    
    private fun processCapturedImage(image: Image) {
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth
            
            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            
            bitmap.copyPixelsFromBuffer(buffer)
            
            val croppedBitmap = if (rowPadding > 0) {
                Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
            } else {
                bitmap
            }
            
            // Notify listener on main thread
            serviceScope.launch(Dispatchers.Main) {
                captureListener?.invoke(croppedBitmap)
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error processing captured image")
        }
    }
    
    fun stopCapture() {
        isCapturing = false
        
        virtualDisplay?.release()
        virtualDisplay = null
        
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null
        
        mediaProjection?.stop()
        mediaProjection = null
        
        stopForeground(true)
        stopSelf()
        
        Timber.d("Screen capture stopped")
    }
    
    private fun createNotification(): Notification {
        val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ACTION_STOP_CAPTURE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis Pattern Overlay")
            .setContentText("Analyzing trading patterns...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    fun setCaptureListener(listener: (Bitmap) -> Unit) {
        captureListener = listener
    }
    
    fun setFPS(newFPS: Int) {
        fps = newFPS.coerceIn(1, 10) // Limit FPS between 1-10
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopCapture()
        serviceScope.cancel()
    }
}