package com.jarvis.patternoverlay.ui

import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.patternoverlay.R
import com.jarvis.patternoverlay.databinding.ActivityCalibrationBinding
import com.jarvis.patternoverlay.model.PriceScale

class CalibrationActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCalibrationBinding
    private var calibrationView: CalibrationOverlayView? = null
    private var cropRect: Rect? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalibrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        createCalibrationOverlay()
    }
    
    private fun setupUI() {
        binding.btnSaveCalibration.setOnClickListener {
            saveCalibration()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
        
        binding.btnReset.setOnClickListener {
            resetCalibration()
        }
    }
    
    private fun createCalibrationOverlay() {
        calibrationView = CalibrationOverlayView(this) { rect ->
            cropRect = rect
            binding.instructionText.text = "Rectangle drawn! Now set the price range for the top and bottom of the chart."
            binding.priceInputLayout.visibility = View.VISIBLE
            binding.btnSaveCalibration.isEnabled = true
        }
        
        setContentView(calibrationView)
    }
    
    private fun saveCalibration() {
        val topPriceText = binding.editTopPrice.text.toString()
        val bottomPriceText = binding.editBottomPrice.text.toString()
        
        if (topPriceText.isEmpty() || bottomPriceText.isEmpty()) {
            Toast.makeText(this, "Please enter both top and bottom prices", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val topPrice = topPriceText.toDouble()
            val bottomPrice = bottomPriceText.toDouble()
            
            if (topPrice <= bottomPrice) {
                Toast.makeText(this, "Top price must be higher than bottom price", Toast.LENGTH_SHORT).show()
                return
            }
            
            val rect = cropRect ?: run {
                Toast.makeText(this, "Please draw a rectangle first", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Save calibration data
            val prefs = getSharedPreferences("calibration_data", MODE_PRIVATE)
            prefs.edit().apply {
                putInt("crop_left", rect.left)
                putInt("crop_top", rect.top)
                putInt("crop_right", rect.right)
                putInt("crop_bottom", rect.bottom)
                putFloat("top_price", topPrice.toFloat())
                putFloat("bottom_price", bottomPrice.toFloat())
                putBoolean("calibration_completed", true)
                apply()
            }
            
            Toast.makeText(this, "Calibration saved successfully!", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
            
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter valid price numbers", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun resetCalibration() {
        calibrationView?.resetSelection()
        cropRect = null
        binding.instructionText.text = getString(R.string.calibration_instruction)
        binding.priceInputLayout.visibility = View.GONE
        binding.btnSaveCalibration.isEnabled = false
        binding.editTopPrice.text?.clear()
        binding.editBottomPrice.text?.clear()
    }
    
    class CalibrationOverlayView(
        context: android.content.Context,
        private val onRectangleDrawn: (Rect) -> Unit
    ) : View(context) {
        
        private val paint = Paint().apply {
            color = Color.CYAN
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        
        private val backgroundPaint = Paint().apply {
            color = Color.argb(100, 0, 255, 255)
        }
        
        private var startX = 0f
        private var startY = 0f
        private var currentX = 0f
        private var currentY = 0f
        private var isDrawing = false
        
        override fun onTouchEvent(event: MotionEvent?): Boolean {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    currentX = event.x
                    currentY = event.y
                    isDrawing = true
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    currentX = event.x
                    currentY = event.y
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    isDrawing = false
                    val rect = Rect(
                        minOf(startX, currentX).toInt(),
                        minOf(startY, currentY).toInt(),
                        maxOf(startX, currentX).toInt(),
                        maxOf(startY, currentY).toInt()
                    )
                    onRectangleDrawn(rect)
                    return true
                }
            }
            return super.onTouchEvent(event)
        }
        
        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)
            
            if (isDrawing) {
                // Draw semi-transparent background
                canvas?.drawRect(
                    minOf(startX, currentX),
                    minOf(startY, currentY),
                    maxOf(startX, currentX),
                    maxOf(startY, currentY),
                    backgroundPaint
                )
                
                // Draw border
                canvas?.drawRect(
                    minOf(startX, currentX),
                    minOf(startY, currentY),
                    maxOf(startX, currentX),
                    maxOf(startY, currentY),
                    paint
                )
            }
        }
        
        fun resetSelection() {
            isDrawing = false
            startX = 0f
            startY = 0f
            currentX = 0f
            currentY = 0f
            invalidate()
        }
    }
}