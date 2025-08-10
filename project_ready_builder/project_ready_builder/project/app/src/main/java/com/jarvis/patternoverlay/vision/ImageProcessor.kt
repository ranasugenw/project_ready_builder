package com.jarvis.patternoverlay.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.jarvis.patternoverlay.model.OHLC
import com.jarvis.patternoverlay.model.PriceScale
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import timber.log.Timber
import java.util.*

class ImageProcessor(private val context: Context) {
    
    private var isOpenCVInitialized = false
    private val tesseractOCR by lazy { TesseractOCR(context) }
    
    fun initializeOpenCV() {
        isOpenCVInitialized = true
        Timber.d("OpenCV initialized for image processing")
    }
    
    fun processScreenCapture(
        bitmap: Bitmap,
        cropArea: Rect,
        priceScale: PriceScale?,
        isDarkTheme: Boolean
    ): List<OHLC> {
        if (!isOpenCVInitialized) {
            Timber.w("OpenCV not initialized, cannot process image")
            return emptyList()
        }
        
        try {
            // Crop the bitmap to chart area
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                cropArea.left,
                cropArea.top,
                cropArea.width(),
                cropArea.height()
            )
            
            // Convert to OpenCV Mat
            val mat = Mat()
            Utils.bitmapToMat(croppedBitmap, mat)
            
            // Preprocess the image
            val processedMat = preprocessImage(mat, isDarkTheme)
            
            // Extract candlesticks
            val candlesticks = extractCandlesticks(processedMat)
            
            // If no price scale provided, try to extract it via OCR
            val scale = priceScale ?: extractPriceScale(mat, cropArea)
            
            // Convert to OHLC data
            return convertToOHLC(candlesticks, scale)
            
        } catch (e: Exception) {
            Timber.e(e, "Error processing screen capture")
            return emptyList()
        }
    }
    
    private fun preprocessImage(mat: Mat, isDarkTheme: Boolean): Mat {
        val gray = Mat()
        val processed = Mat()
        
        // Convert to grayscale
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        
        // Apply adaptive thresholding based on theme
        if (isDarkTheme) {
            // For dark themes, invert and threshold
            Core.bitwise_not(gray, processed)
            Imgproc.adaptiveThreshold(
                processed, processed, 255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,
                11, 2.0
            )
        } else {
            // For light themes, direct threshold
            Imgproc.adaptiveThreshold(
                gray, processed, 255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,
                11, 2.0
            )
        }
        
        // Morphological operations to clean up
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.morphologyEx(processed, processed, Imgproc.MORPH_CLOSE, kernel)
        
        return processed
    }
    
    private fun extractCandlesticks(mat: Mat): List<CandlestickData> {
        val candlesticks = mutableListOf<CandlestickData>()
        val contours = mutableListOf<MatOfPoint>()
        
        // Find contours
        Imgproc.findContours(mat, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        
        // Group contours into candlesticks
        val sortedContours = contours.sortedBy { Imgproc.boundingRect(it).x }
        
        var currentX = -1
        var currentGroup = mutableListOf<MatOfPoint>()
        
        for (contour in sortedContours) {
            val rect = Imgproc.boundingRect(contour)
            
            // Group contours by X position (allowing some tolerance)
            if (currentX == -1 || kotlin.math.abs(rect.x - currentX) <= 10) {
                currentGroup.add(contour)
                currentX = rect.x
            } else {
                // Process the current group as a candlestick
                if (currentGroup.isNotEmpty()) {
                    val candlestick = processCandlestickGroup(currentGroup, currentX)
                    if (candlestick != null) {
                        candlesticks.add(candlestick)
                    }
                }
                
                // Start new group
                currentGroup.clear()
                currentGroup.add(contour)
                currentX = rect.x
            }
        }
        
        // Process the last group
        if (currentGroup.isNotEmpty()) {
            val candlestick = processCandlestickGroup(currentGroup, currentX)
            if (candlestick != null) {
                candlesticks.add(candlestick)
            }
        }
        
        return candlesticks
    }
    
    private fun processCandlestickGroup(contours: List<MatOfPoint>, centerX: Int): CandlestickData? {
        if (contours.isEmpty()) return null
        
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        var bodyTop = Float.MAX_VALUE
        var bodyBottom = Float.MIN_VALUE
        
        // Find the overall bounds and identify body vs wick
        for (contour in contours) {
            val rect = Imgproc.boundingRect(contour)
            val area = Imgproc.contourArea(contour)
            
            minY = kotlin.math.min(minY, rect.y.toFloat())
            maxY = kotlin.math.max(maxY, (rect.y + rect.height).toFloat())
            
            // Assume thicker contours are the body
            if (area > 50 || rect.width > 3) { // Body detection heuristics
                bodyTop = kotlin.math.min(bodyTop, rect.y.toFloat())
                bodyBottom = kotlin.math.max(bodyBottom, (rect.y + rect.height).toFloat())
            }
        }
        
        return CandlestickData(
            centerX = centerX.toFloat(),
            wickTop = minY,
            wickBottom = maxY,
            bodyTop = if (bodyTop != Float.MAX_VALUE) bodyTop else minY,
            bodyBottom = if (bodyBottom != Float.MIN_VALUE) bodyBottom else maxY
        )
    }
    
    private fun extractPriceScale(mat: Mat, cropArea: Rect): PriceScale? {
        return try {
            // Try to extract price labels using OCR on the Y-axis area
            val yAxisArea = Rect(cropArea.width() - 100, 0, 100, cropArea.height())
            val yAxisBitmap = Bitmap.createBitmap(100, cropArea.height(), Bitmap.Config.ARGB_8888)
            
            val yAxisMat = Mat(mat, org.opencv.core.Rect(
                mat.width() - 100, 0, 100, mat.height()
            ))
            
            Utils.matToBitmap(yAxisMat, yAxisBitmap)
            val priceLabels = tesseractOCR.extractPriceLabels(yAxisBitmap)
            
            if (priceLabels.size >= 2) {
                val topPrice = priceLabels.maxOrNull() ?: return null
                val bottomPrice = priceLabels.minOrNull() ?: return null
                
                PriceScale(
                    minPrice = bottomPrice,
                    maxPrice = topPrice,
                    topPixel = 0f,
                    bottomPixel = cropArea.height().toFloat()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error extracting price scale")
            null
        }
    }
    
    private fun convertToOHLC(candlesticks: List<CandlestickData>, priceScale: PriceScale?): List<OHLC> {
        if (priceScale == null) {
            Timber.w("No price scale available, cannot convert to OHLC")
            return emptyList()
        }
        
        return candlesticks.mapIndexed { index, candle ->
            val timestamp = System.currentTimeMillis() - (candlesticks.size - index) * 60000L
            
            // Determine open/close based on body position
            val isGreen = candle.bodyTop > candle.bodyBottom
            val openPixel = if (isGreen) candle.bodyBottom else candle.bodyTop
            val closePixel = if (isGreen) candle.bodyTop else candle.bodyBottom
            
            OHLC.fromPixelData(
                timestamp = timestamp,
                candleTop = candle.bodyTop,
                candleBottom = candle.bodyBottom,
                wickTop = candle.wickTop,
                wickBottom = candle.wickBottom,
                openPixel = openPixel,
                closePixel = closePixel,
                priceScale = priceScale
            )
        }
    }
    
    data class CandlestickData(
        val centerX: Float,
        val wickTop: Float,
        val wickBottom: Float,
        val bodyTop: Float,
        val bodyBottom: Float
    )
}