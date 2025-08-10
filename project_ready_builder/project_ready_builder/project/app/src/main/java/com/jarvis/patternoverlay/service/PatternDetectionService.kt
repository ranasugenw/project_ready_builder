package com.jarvis.patternoverlay.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jarvis.patternoverlay.JarvisApplication
import com.jarvis.patternoverlay.R
import kotlinx.coroutines.*
import timber.log.Timber

class PatternDetectionService : Service() {
    
    private var serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    
    companion object {
        private const val NOTIFICATION_ID = 1002
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        Timber.d("Pattern Detection Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startAnalysis()
        }
        return START_STICKY
    }
    
    private fun startAnalysis() {
        isRunning = true
        
        val notification = NotificationCompat.Builder(this, "screen_capture_channel")
            .setContentTitle("Pattern Analysis")
            .setContentText("Analyzing trading patterns...")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
        
        serviceScope.launch {
            while (isRunning) {
                try {
                    // This service coordinates with the main app
                    // The actual pattern detection happens in MainViewModel
                    // when screen captures are processed
                    
                    delay(5000) // Check every 5 seconds
                    
                    // Clean up old patterns from database
                    val app = application as JarvisApplication
                    val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours ago
                    app.patternRepository.deleteOldPatterns(cutoffTime)
                    
                } catch (e: Exception) {
                    Timber.e(e, "Error in pattern detection service")
                }
            }
        }
        
        Timber.d("Pattern analysis started")
    }
    
    private fun stopAnalysis() {
        isRunning = false
        stopForeground(true)
        stopSelf()
        Timber.d("Pattern analysis stopped")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAnalysis()
        serviceScope.cancel()
    }
}