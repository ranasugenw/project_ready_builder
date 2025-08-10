package com.jarvis.patternoverlay.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.IBinder
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import com.jarvis.patternoverlay.R
import com.jarvis.patternoverlay.model.Pattern
import com.jarvis.patternoverlay.model.PatternSignal
import kotlinx.coroutines.*
import timber.log.Timber

class OverlayService : Service() {
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var drawingView: DrawingView? = null
    private var controlPanel: LinearLayout? = null
    
    private var isVisible = true
    private var isMinimized = false
    private var currentPatterns = listOf<Pattern>()
    private var currentSignal: PatternSignal? = null
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createOverlay()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "SHOW_OVERLAY" -> showOverlay()
            "HIDE_OVERLAY" -> hideOverlay()
            "UPDATE_PATTERNS" -> {
                val patterns = intent.getParcelableArrayListExtra<Pattern>("patterns")
                val signal = intent.getParcelableExtra<PatternSignal>("signal")
                updatePatterns(patterns ?: emptyList(), signal)
            }
        }
        return START_STICKY
    }
    
    private fun createOverlay() {
        try {
            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            
            overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
            drawingView = overlayView?.findViewById(R.id.drawing_view)
            controlPanel = overlayView?.findViewById(R.id.control_panel)
            
            setupControlPanel()
            
            windowManager?.addView(overlayView, layoutParams)
            
            Timber.d("Overlay created successfully")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to create overlay")
        }
    }
    
    private fun setupControlPanel() {
        controlPanel?.findViewById<ImageButton>(R.id.btn_minimize)?.setOnClickListener {
            toggleMinimize()
        }
        
        controlPanel?.findViewById<ImageButton>(R.id.btn_hide)?.setOnClickListener {
            hideOverlay()
        }
        
        controlPanel?.findViewById<ImageButton>(R.id.btn_settings)?.setOnClickListener {
            openSettings()
        }
        
        // Make control panel draggable
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        controlPanel?.setOnTouchListener { _, event ->
            val layoutParams = overlayView?.layoutParams as? WindowManager.LayoutParams
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams?.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams?.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(overlayView, layoutParams)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun toggleMinimize() {
        isMinimized = !isMinimized
        if (isMinimized) {
            drawingView?.visibility = View.GONE
            controlPanel?.findViewById<TextView>(R.id.signal_info)?.visibility = View.GONE
        } else {
            drawingView?.visibility = View.VISIBLE
            controlPanel?.findViewById<TextView>(R.id.signal_info)?.visibility = View.VISIBLE
        }
    }
    
    private fun showOverlay() {
        isVisible = true
        overlayView?.visibility = View.VISIBLE
    }
    
    private fun hideOverlay() {
        isVisible = false
        overlayView?.visibility = View.GONE
    }
    
    private fun updatePatterns(patterns: List<Pattern>, signal: PatternSignal?) {
        currentPatterns = patterns
        currentSignal = signal
        
        drawingView?.updatePatterns(patterns)
        updateSignalInfo(signal)
    }
    
    private fun updateSignalInfo(signal: PatternSignal?) {
        val signalInfoText = controlPanel?.findViewById<TextView>(R.id.signal_info)
        
        if (signal != null) {
            val signalText = """
                ${signal.action} | ${String.format("%.1f%%", signal.confidence * 100)}
                Entry: ${String.format("%.4f", signal.entryPrice)}
                SL: ${String.format("%.4f", signal.stopLoss)}
                TP1: ${String.format("%.4f", signal.target1)}
                R:R ${String.format("%.1f", signal.riskRewardRatio)}
            """.trimIndent()
            
            signalInfoText?.text = signalText
            signalInfoText?.setTextColor(
                when (signal.action) {
                    com.jarvis.patternoverlay.model.SignalAction.BUY -> Color.GREEN
                    com.jarvis.patternoverlay.model.SignalAction.SELL -> Color.RED
                    else -> Color.YELLOW
                }
            )
        } else {
            signalInfoText?.text = "No Signal"
            signalInfoText?.setTextColor(Color.GRAY)
        }
    }
    
    private fun openSettings() {
        val intent = Intent(this, com.jarvis.patternoverlay.ui.SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager?.removeView(it) }
    }
    
    class DrawingView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {
        
        private val paint = Paint().apply {
            isAntiAlias = true
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        
        private val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 24f
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
        }
        
        private var patterns = listOf<Pattern>()
        
        fun updatePatterns(newPatterns: List<Pattern>) {
            patterns = newPatterns
            invalidate()
        }
        
        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)
            if (canvas == null) return
            
            // Draw semi-transparent background for better visibility
            canvas.drawColor(Color.argb(30, 0, 0, 0))
            
            for (pattern in patterns.take(5)) { // Limit to top 5 patterns
                drawPattern(canvas, pattern)
            }
        }
        
        private fun drawPattern(canvas: Canvas, pattern: Pattern) {
            // Set color based on pattern direction
            paint.color = when (pattern.direction) {
                com.jarvis.patternoverlay.model.PatternDirection.BULLISH -> Color.GREEN
                com.jarvis.patternoverlay.model.PatternDirection.BEARISH -> Color.RED
                else -> Color.YELLOW
            }
            
            // For simplicity, draw a rectangle representing the pattern area
            // In a real implementation, you would map pattern coordinates to screen coordinates
            val left = width * 0.1f
            val top = height * 0.2f
            val right = width * 0.9f
            val bottom = height * 0.8f
            
            // Draw pattern outline
            canvas.drawRect(left, top, right, bottom, paint)
            
            // Draw pattern label
            val label = "${pattern.name} (${String.format("%.1f%%", pattern.confidence * 100)})"
            canvas.drawText(label, left + 20, top - 10, textPaint)
            
            // Draw entry and target lines
            val entryY = top + (bottom - top) * 0.5f
            val slY = if (pattern.direction == com.jarvis.patternoverlay.model.PatternDirection.BULLISH) bottom else top
            val tp1Y = if (pattern.direction == com.jarvis.patternoverlay.model.PatternDirection.BULLISH) top else bottom
            
            // Entry line (blue)
            paint.color = Color.BLUE
            canvas.drawLine(left, entryY, right, entryY, paint)
            canvas.drawText("Entry", right + 10, entryY, textPaint)
            
            // Stop loss line (red)
            paint.color = Color.RED
            paint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            canvas.drawLine(left, slY, right, slY, paint)
            canvas.drawText("SL", right + 10, slY, textPaint)
            
            // Target line (green)
            paint.color = Color.GREEN
            canvas.drawLine(left, tp1Y, right, tp1Y, paint)
            canvas.drawText("TP1", right + 10, tp1Y, textPaint)
            
            paint.pathEffect = null // Reset dash effect
        }
    }
}