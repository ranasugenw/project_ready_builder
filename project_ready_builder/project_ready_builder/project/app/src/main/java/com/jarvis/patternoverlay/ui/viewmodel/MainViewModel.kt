package com.jarvis.patternoverlay.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.patternoverlay.data.repository.PatternRepository
import com.jarvis.patternoverlay.engine.IndicatorEngine
import com.jarvis.patternoverlay.engine.PatternEngine
import com.jarvis.patternoverlay.model.*
import com.jarvis.patternoverlay.vision.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class MainViewModel(
    private val patternRepository: PatternRepository,
    private val indicatorEngine: IndicatorEngine,
    private val patternEngine: PatternEngine,
    private val imageProcessor: ImageProcessor
) : ViewModel() {
    
    private val _isAnalyzing = MutableLiveData<Boolean>(false)
    val isAnalyzing: LiveData<Boolean> = _isAnalyzing
    
    private val _detectedPatterns = MutableLiveData<List<Pattern>>(emptyList())
    val detectedPatterns: LiveData<List<Pattern>> = _detectedPatterns
    
    private val _currentSignal = MutableLiveData<PatternSignal?>(null)
    val currentSignal: LiveData<PatternSignal?> = _currentSignal
    
    private val _ohlcData = MutableLiveData<List<OHLC>>(emptyList())
    val ohlcData: LiveData<List<OHLC>> = _ohlcData
    
    // Settings
    private var cropArea: Rect = Rect(0, 0, 1080, 1920) // Default crop area
    private var priceScale: PriceScale? = null
    private var isDarkTheme = true
    private var currentTimeframe = "1m"
    private var minConfidence = 0.6
    
    fun startAnalysis() {
        _isAnalyzing.value = true
        Timber.d("Pattern analysis started")
    }
    
    fun stopAnalysis() {
        _isAnalyzing.value = false
        _detectedPatterns.value = emptyList()
        _currentSignal.value = null
        Timber.d("Pattern analysis stopped")
    }
    
    fun processCapturedScreen(bitmap: Bitmap) {
        if (_isAnalyzing.value != true) return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Process the captured screen
                val newOhlcData = imageProcessor.processScreenCapture(
                    bitmap = bitmap,
                    cropArea = cropArea,
                    priceScale = priceScale,
                    isDarkTheme = isDarkTheme
                )
                
                if (newOhlcData.isNotEmpty()) {
                    // Update OHLC data (keep last 200 candles for analysis)
                    val currentData = _ohlcData.value ?: emptyList()
                    val combinedData = (currentData + newOhlcData).takeLast(200)
                    
                    withContext(Dispatchers.Main) {
                        _ohlcData.value = combinedData
                    }
                    
                    // Detect patterns
                    val patterns = patternEngine.detectPatterns(combinedData, currentTimeframe)
                    
                    // Filter by minimum confidence
                    val filteredPatterns = patterns.filter { it.confidence >= minConfidence }
                    
                    // Generate signal
                    val signal = patternEngine.generateSignal(filteredPatterns, combinedData, currentTimeframe)
                    
                    // Save patterns to database
                    filteredPatterns.forEach { pattern ->
                        patternRepository.insertPattern(pattern)
                    }
                    
                    // Update UI on main thread
                    withContext(Dispatchers.Main) {
                        _detectedPatterns.value = filteredPatterns
                        _currentSignal.value = signal
                    }
                    
                    Timber.d("Processed screen capture: ${newOhlcData.size} candles, ${filteredPatterns.size} patterns")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Error processing captured screen")
            }
        }
    }
    
    fun updateCropArea(rect: Rect) {
        cropArea = rect
        Timber.d("Crop area updated: $rect")
    }
    
    fun updatePriceScale(scale: PriceScale) {
        priceScale = scale
        Timber.d("Price scale updated: $scale")
    }
    
    fun updateSettings(
        darkTheme: Boolean,
        timeframe: String,
        confidence: Double
    ) {
        isDarkTheme = darkTheme
        currentTimeframe = timeframe
        minConfidence = confidence
        Timber.d("Settings updated: theme=$darkTheme, timeframe=$timeframe, confidence=$confidence")
    }
    
    fun getIndicatorValues(indicatorType: String, period: Int = 14): List<Double> {
        val ohlc = _ohlcData.value ?: return emptyList()
        val closes = ohlc.map { it.close }
        
        return when (indicatorType.lowercase()) {
            "rsi" -> indicatorEngine.rsi(closes, period)
            "sma" -> indicatorEngine.sma(closes, period)
            "ema" -> indicatorEngine.ema(closes, period)
            "atr" -> indicatorEngine.atr(ohlc, period)
            else -> emptyList()
        }
    }
    
    fun exportPatterns(): List<Pattern> {
        return _detectedPatterns.value ?: emptyList()
    }
    
    fun clearData() {
        _ohlcData.value = emptyList()
        _detectedPatterns.value = emptyList()
        _currentSignal.value = null
    }
}