package com.jarvis.patternoverlay.engine

import android.content.Context
import com.jarvis.patternoverlay.model.*
import org.tensorflow.lite.Interpreter
import timber.log.Timber
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PatternEngine(private val context: Context) {
    
    private var tfliteInterpreter: Interpreter? = null
    private val indicatorEngine = IndicatorEngine()
    
    init {
        loadTFLiteModel()
    }
    
    private fun loadTFLiteModel() {
        try {
            val modelBuffer = loadModelFile("pattern_classifier.tflite")
            tfliteInterpreter = Interpreter(modelBuffer)
            Timber.d("TensorFlow Lite model loaded successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load TensorFlow Lite model")
        }
    }
    
    private fun loadModelFile(filename: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    fun detectPatterns(ohlc: List<OHLC>, timeframe: String): List<Pattern> {
        if (ohlc.size < 10) return emptyList()
        
        val patterns = mutableListOf<Pattern>()
        
        // Detect candlestick patterns
        patterns.addAll(detectCandlestickPatterns(ohlc, timeframe))
        
        // Detect chart patterns
        patterns.addAll(detectChartPatterns(ohlc, timeframe))
        
        // Detect ML patterns
        patterns.addAll(detectMLPatterns(ohlc, timeframe))
        
        return patterns.sortedByDescending { it.confidence }
    }
    
    private fun detectCandlestickPatterns(ohlc: List<OHLC>, timeframe: String): List<Pattern> {
        val patterns = mutableListOf<Pattern>()
        
        for (i in 2 until ohlc.size) {
            val current = ohlc[i]
            val prev1 = ohlc[i - 1]
            val prev2 = if (i >= 2) ohlc[i - 2] else null
            
            // Doji Pattern
            detectDoji(current, i, timeframe)?.let { patterns.add(it) }
            
            // Hammer Pattern
            detectHammer(current, i, timeframe)?.let { patterns.add(it) }
            
            // Shooting Star Pattern
            detectShootingStar(current, i, timeframe)?.let { patterns.add(it) }
            
            // Engulfing Patterns
            detectEngulfing(current, prev1, i, timeframe)?.let { patterns.add(it) }
            
            // Morning/Evening Star (requires 3 candles)
            if (prev2 != null) {
                detectMorningStar(current, prev1, prev2, i, timeframe)?.let { patterns.add(it) }
                detectEveningStar(current, prev1, prev2, i, timeframe)?.let { patterns.add(it) }
            }
            
            // Three White Soldiers / Three Black Crows (requires more history)
            if (i >= 4) {
                detectThreeWhiteSoldiers(ohlc.subList(i - 2, i + 1), i, timeframe)?.let { patterns.add(it) }
                detectThreeBlackCrows(ohlc.subList(i - 2, i + 1), i, timeframe)?.let { patterns.add(it) }
            }
        }
        
        return patterns
    }
    
    private fun detectDoji(candle: OHLC, index: Int, timeframe: String): Pattern? {
        val bodySize = candle.bodySize
        val totalRange = candle.totalRange
        
        // Doji: body is less than 10% of total range
        if (bodySize < totalRange * 0.1 && totalRange > 0) {
            val confidence = 1.0 - (bodySize / totalRange) // Higher confidence for smaller body
            
            return Pattern(
                name = "Doji",
                type = PatternType.DOJI,
                timeframe = timeframe,
                timestamp = candle.timestamp,
                confidence = confidence,
                startIndex = index,
                endIndex = index,
                direction = PatternDirection.NEUTRAL,
                entryPrice = candle.close,
                stopLoss = if (candle.isBullish) candle.low * 0.98 else candle.high * 1.02,
                target1 = candle.close * if (candle.isBullish) 1.015 else 0.985,
                target2 = candle.close * if (candle.isBullish) 1.03 else 0.97,
                expectedDuration = "1-3 candles",
                probability = 0.5,
                riskRewardRatio = 2.0
            )
        }
        
        return null
    }
    
    private fun detectHammer(candle: OHLC, index: Int, timeframe: String): Pattern? {
        val bodySize = candle.bodySize
        val lowerWick = candle.lowerWick
        val upperWick = candle.upperWick
        val totalRange = candle.totalRange
        
        // Hammer: small body, long lower wick, short upper wick
        if (bodySize < totalRange * 0.3 && 
            lowerWick > bodySize * 2 && 
            upperWick < bodySize * 0.5 &&
            totalRange > 0) {
            
            val confidence = (lowerWick / totalRange) * 0.8 + 0.2
            
            return Pattern(
                name = "Hammer",
                type = PatternType.HAMMER,
                timeframe = timeframe,
                timestamp = candle.timestamp,
                confidence = confidence,
                startIndex = index,
                endIndex = index,
                direction = PatternDirection.BULLISH,
                entryPrice = candle.high * 1.001, // Entry slightly above high
                stopLoss = candle.low * 0.995,
                target1 = candle.close * 1.02,
                target2 = candle.close * 1.04,
                expectedDuration = "2-5 candles",
                probability = 0.65,
                riskRewardRatio = 2.5
            )
        }
        
        return null
    }
    
    private fun detectShootingStar(candle: OHLC, index: Int, timeframe: String): Pattern? {
        val bodySize = candle.bodySize
        val lowerWick = candle.lowerWick
        val upperWick = candle.upperWick
        val totalRange = candle.totalRange
        
        // Shooting Star: small body, long upper wick, short lower wick
        if (bodySize < totalRange * 0.3 && 
            upperWick > bodySize * 2 && 
            lowerWick < bodySize * 0.5 &&
            totalRange > 0) {
            
            val confidence = (upperWick / totalRange) * 0.8 + 0.2
            
            return Pattern(
                name = "Shooting Star",
                type = PatternType.SHOOTING_STAR,
                timeframe = timeframe,
                timestamp = candle.timestamp,
                confidence = confidence,
                startIndex = index,
                endIndex = index,
                direction = PatternDirection.BEARISH,
                entryPrice = candle.low * 0.999, // Entry slightly below low
                stopLoss = candle.high * 1.005,
                target1 = candle.close * 0.98,
                target2 = candle.close * 0.96,
                expectedDuration = "2-5 candles",
                probability = 0.65,
                riskRewardRatio = 2.5
            )
        }
        
        return null
    }
    
    private fun detectEngulfing(current: OHLC, previous: OHLC, index: Int, timeframe: String): Pattern? {
        // Bullish Engulfing
        if (!previous.isBullish && current.isBullish &&
            current.open < previous.close && current.close > previous.open) {
            
            val engulfmentRatio = current.bodySize / previous.bodySize
            val confidence = min(engulfmentRatio * 0.5, 0.9)
            
            return Pattern(
                name = "Bullish Engulfing",
                type = PatternType.ENGULFING_BULLISH,
                timeframe = timeframe,
                timestamp = current.timestamp,
                confidence = confidence,
                startIndex = index - 1,
                endIndex = index,
                direction = PatternDirection.BULLISH,
                entryPrice = current.close * 1.002,
                stopLoss = min(current.low, previous.low) * 0.98,
                target1 = current.close * 1.025,
                target2 = current.close * 1.05,
                expectedDuration = "3-8 candles",
                probability = 0.72,
                riskRewardRatio = 3.0
            )
        }
        
        // Bearish Engulfing
        if (previous.isBullish && !current.isBullish &&
            current.open > previous.close && current.close < previous.open) {
            
            val engulfmentRatio = current.bodySize / previous.bodySize
            val confidence = min(engulfmentRatio * 0.5, 0.9)
            
            return Pattern(
                name = "Bearish Engulfing",
                type = PatternType.ENGULFING_BEARISH,
                timeframe = timeframe,
                timestamp = current.timestamp,
                confidence = confidence,
                startIndex = index - 1,
                endIndex = index,
                direction = PatternDirection.BEARISH,
                entryPrice = current.close * 0.998,
                stopLoss = max(current.high, previous.high) * 1.02,
                target1 = current.close * 0.975,
                target2 = current.close * 0.95,
                expectedDuration = "3-8 candles",
                probability = 0.72,
                riskRewardRatio = 3.0
            )
        }
        
        return null
    }
    
    private fun detectMorningStar(current: OHLC, middle: OHLC, first: OHLC, index: Int, timeframe: String): Pattern? {
        // Morning Star: bearish candle, small body/doji, bullish candle
        if (!first.isBullish && current.isBullish &&
            middle.bodySize < first.bodySize * 0.5 &&
            middle.bodySize < current.bodySize * 0.5 &&
            current.close > (first.open + first.close) / 2) {
            
            val confidence = 0.7 + (current.bodySize / first.bodySize) * 0.2
            
            return Pattern(
                name = "Morning Star",
                type = PatternType.MORNING_STAR,
                timeframe = timeframe,
                timestamp = current.timestamp,
                confidence = min(confidence, 0.95),
                startIndex = index - 2,
                endIndex = index,
                direction = PatternDirection.BULLISH,
                entryPrice = current.close * 1.005,
                stopLoss = min(first.low, min(middle.low, current.low)) * 0.97,
                target1 = current.close * 1.03,
                target2 = current.close * 1.06,
                expectedDuration = "5-12 candles",
                probability = 0.78,
                riskRewardRatio = 3.5
            )
        }
        
        return null
    }
    
    private fun detectEveningStar(current: OHLC, middle: OHLC, first: OHLC, index: Int, timeframe: String): Pattern? {
        // Evening Star: bullish candle, small body/doji, bearish candle
        if (first.isBullish && !current.isBullish &&
            middle.bodySize < first.bodySize * 0.5 &&
            middle.bodySize < current.bodySize * 0.5 &&
            current.close < (first.open + first.close) / 2) {
            
            val confidence = 0.7 + (current.bodySize / first.bodySize) * 0.2
            
            return Pattern(
                name = "Evening Star",
                type = PatternType.EVENING_STAR,
                timeframe = timeframe,
                timestamp = current.timestamp,
                confidence = min(confidence, 0.95),
                startIndex = index - 2,
                endIndex = index,
                direction = PatternDirection.BEARISH,
                entryPrice = current.close * 0.995,
                stopLoss = max(first.high, max(middle.high, current.high)) * 1.03,
                target1 = current.close * 0.97,
                target2 = current.close * 0.94,
                expectedDuration = "5-12 candles",
                probability = 0.78,
                riskRewardRatio = 3.5
            )
        }
        
        return null
    }
    
    private fun detectThreeWhiteSoldiers(candles: List<OHLC>, index: Int, timeframe: String): Pattern? {
        if (candles.size != 3) return null
        
        // All three candles should be bullish with increasing closes
        if (candles.all { it.isBullish } &&
            candles[1].close > candles[0].close &&
            candles[2].close > candles[1].close &&
            candles.all { it.bodySize > it.totalRange * 0.6 }) {
            
            val consistency = candles.map { it.bodySize / it.totalRange }.average()
            val confidence = consistency * 0.8
            
            return Pattern(
                name = "Three White Soldiers",
                type = PatternType.THREE_WHITE_SOLDIERS,
                timeframe = timeframe,
                timestamp = candles.last().timestamp,
                confidence = confidence,
                startIndex = index - 2,
                endIndex = index,
                direction = PatternDirection.BULLISH,
                entryPrice = candles.last().close * 1.003,
                stopLoss = candles.minOf { it.low } * 0.975,
                target1 = candles.last().close * 1.04,
                target2 = candles.last().close * 1.08,
                expectedDuration = "5-15 candles",
                probability = 0.75,
                riskRewardRatio = 4.0
            )
        }
        
        return null
    }
    
    private fun detectThreeBlackCrows(candles: List<OHLC>, index: Int, timeframe: String): Pattern? {
        if (candles.size != 3) return null
        
        // All three candles should be bearish with decreasing closes
        if (candles.all { it.isBearish } &&
            candles[1].close < candles[0].close &&
            candles[2].close < candles[1].close &&
            candles.all { it.bodySize > it.totalRange * 0.6 }) {
            
            val consistency = candles.map { it.bodySize / it.totalRange }.average()
            val confidence = consistency * 0.8
            
            return Pattern(
                name = "Three Black Crows",
                type = PatternType.THREE_BLACK_CROWS,
                timeframe = timeframe,
                timestamp = candles.last().timestamp,
                confidence = confidence,
                startIndex = index - 2,
                endIndex = index,
                direction = PatternDirection.BEARISH,
                entryPrice = candles.last().close * 0.997,
                stopLoss = candles.maxOf { it.high } * 1.025,
                target1 = candles.last().close * 0.96,
                target2 = candles.last().close * 0.92,
                expectedDuration = "5-15 candles",
                probability = 0.75,
                riskRewardRatio = 4.0
            )
        }
        
        return null
    }
    
    private fun detectChartPatterns(ohlc: List<OHLC>, timeframe: String): List<Pattern> {
        val patterns = mutableListOf<Pattern>()
        
        if (ohlc.size >= 20) {
            // Detect double top/bottom patterns
            patterns.addAll(detectDoubleTopBottom(ohlc, timeframe))
            
            // Detect triangles
            patterns.addAll(detectTriangles(ohlc, timeframe))
            
            // Detect channels
            patterns.addAll(detectChannels(ohlc, timeframe))
        }
        
        return patterns
    }
    
    private fun detectDoubleTopBottom(ohlc: List<OHLC>, timeframe: String): List<Pattern> {
        val patterns = mutableListOf<Pattern>()
        val window = 20
        
        for (i in window until ohlc.size - window) {
            val highs = ohlc.subList(i - window, i + window + 1).map { it.high }
            val lows = ohlc.subList(i - window, i + window + 1).map { it.low }
            
            // Double Top Detection
            val maxHigh = highs.maxOrNull() ?: continue
            val highIndices = highs.mapIndexedNotNull { index, high -> 
                if (high == maxHigh) index else null 
            }
            
            if (highIndices.size >= 2) {
                val firstPeak = highIndices[0] + i - window
                val secondPeak = highIndices.last() + i - window
                
                if (secondPeak - firstPeak >= 10) {
                    val valley = ohlc.subList(firstPeak, secondPeak + 1).minOf { it.low }
                    val resistance = maxHigh
                    val support = valley
                    
                    if ((resistance - support) / resistance > 0.02) { // At least 2% difference
                        patterns.add(Pattern(
                            name = "Double Top",
                            type = PatternType.DOUBLE_TOP,
                            timeframe = timeframe,
                            timestamp = ohlc[secondPeak].timestamp,
                            confidence = 0.7,
                            startIndex = firstPeak,
                            endIndex = secondPeak,
                            direction = PatternDirection.BEARISH,
                            entryPrice = support,
                            stopLoss = resistance * 1.01,
                            target1 = support - (resistance - support) * 0.5,
                            target2 = support - (resistance - support),
                            expectedDuration = "10-30 candles",
                            probability = 0.68,
                            riskRewardRatio = 2.8
                        ))
                    }
                }
            }
        }
        
        return patterns
    }
    
    private fun detectTriangles(ohlc: List<OHLC>, timeframe: String): List<Pattern> {
        val patterns = mutableListOf<Pattern>()
        val window = 30
        
        for (i in window until ohlc.size - 10) {
            val subset = ohlc.subList(i - window, i)
            val highs = subset.map { it.high }
            val lows = subset.map { it.low }
            
            // Simple trend line analysis
            val highTrend = calculateTrendSlope(highs)
            val lowTrend = calculateTrendSlope(lows)
            
            // Ascending Triangle: horizontal resistance, rising support
            if (abs(highTrend) < 0.001 && lowTrend > 0.001) {
                patterns.add(Pattern(
                    name = "Ascending Triangle",
                    type = PatternType.ASCENDING_TRIANGLE,
                    timeframe = timeframe,
                    timestamp = ohlc[i].timestamp,
                    confidence = 0.65,
                    startIndex = i - window,
                    endIndex = i,
                    direction = PatternDirection.BULLISH,
                    entryPrice = highs.maxOrNull() ?: ohlc[i].close,
                    stopLoss = lows.minOrNull() ?: ohlc[i].low,
                    target1 = ohlc[i].close * 1.03,
                    target2 = ohlc[i].close * 1.06,
                    expectedDuration = "5-20 candles",
                    probability = 0.72,
                    riskRewardRatio = 2.5
                ))
            }
            
            // Descending Triangle: falling resistance, horizontal support
            if (highTrend < -0.001 && abs(lowTrend) < 0.001) {
                patterns.add(Pattern(
                    name = "Descending Triangle",
                    type = PatternType.DESCENDING_TRIANGLE,
                    timeframe = timeframe,
                    timestamp = ohlc[i].timestamp,
                    confidence = 0.65,
                    startIndex = i - window,
                    endIndex = i,
                    direction = PatternDirection.BEARISH,
                    entryPrice = lows.minOrNull() ?: ohlc[i].close,
                    stopLoss = highs.maxOrNull() ?: ohlc[i].high,
                    target1 = ohlc[i].close * 0.97,
                    target2 = ohlc[i].close * 0.94,
                    expectedDuration = "5-20 candles",
                    probability = 0.72,
                    riskRewardRatio = 2.5
                ))
            }
        }
        
        return patterns
    }
    
    private fun detectChannels(ohlc: List<OHLC>, timeframe: String): List<Pattern> {
        val patterns = mutableListOf<Pattern>()
        val window = 25
        
        for (i in window until ohlc.size - 5) {
            val subset = ohlc.subList(i - window, i)
            val highs = subset.map { it.high }
            val lows = subset.map { it.low }
            
            val highTrend = calculateTrendSlope(highs)
            val lowTrend = calculateTrendSlope(lows)
            
            // Parallel channel detection
            if (abs(highTrend - lowTrend) < 0.0005 && abs(highTrend) > 0.001) {
                val direction = if (highTrend > 0) PatternDirection.BULLISH else PatternDirection.BEARISH
                val patternType = if (highTrend > 0) PatternType.CHANNEL_UP else PatternType.CHANNEL_DOWN
                val patternName = if (highTrend > 0) "Rising Channel" else "Falling Channel"
                
                patterns.add(Pattern(
                    name = patternName,
                    type = patternType,
                    timeframe = timeframe,
                    timestamp = ohlc[i].timestamp,
                    confidence = 0.6,
                    startIndex = i - window,
                    endIndex = i,
                    direction = direction,
                    entryPrice = ohlc[i].close,
                    stopLoss = if (direction == PatternDirection.BULLISH) lows.minOrNull() ?: ohlc[i].low else highs.maxOrNull() ?: ohlc[i].high,
                    target1 = if (direction == PatternDirection.BULLISH) ohlc[i].close * 1.02 else ohlc[i].close * 0.98,
                    target2 = if (direction == PatternDirection.BULLISH) ohlc[i].close * 1.04 else ohlc[i].close * 0.96,
                    expectedDuration = "8-25 candles",
                    probability = 0.65,
                    riskRewardRatio = 2.2
                ))
            }
        }
        
        return patterns
    }
    
    private fun calculateTrendSlope(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val n = values.size
        val sumX = (0 until n).sum().toDouble()
        val sumY = values.sum()
        val sumXY = values.mapIndexed { index, value -> index * value }.sum()
        val sumX2 = (0 until n).sumOf { it * it }.toDouble()
        
        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
    }
    
    private fun detectMLPatterns(ohlc: List<OHLC>, timeframe: String): List<Pattern> {
        val tflite = tfliteInterpreter ?: return emptyList()
        
        if (ohlc.size < 50) return emptyList()
        
        try {
            // Prepare input features (last 50 candles normalized)
            val features = prepareMLFeatures(ohlc.takeLast(50))
            val input = Array(1) { features }
            val output = Array(1) { FloatArray(4) } // [bullish, bearish, continuation, reversal]
            
            tflite.run(input, output)
            
            val predictions = output[0]
            val maxConfidence = predictions.maxOrNull() ?: 0f
            val maxIndex = predictions.indexOf(maxConfidence)
            
            if (maxConfidence > 0.7) { // Only high confidence predictions
                val patternType = when (maxIndex) {
                    0 -> PatternType.ML_BULLISH
                    1 -> PatternType.ML_BEARISH
                    2 -> PatternType.ML_CONTINUATION
                    else -> PatternType.ML_REVERSAL
                }
                
                val direction = when (maxIndex) {
                    0 -> PatternDirection.BULLISH
                    1 -> PatternDirection.BEARISH
                    else -> PatternDirection.NEUTRAL
                }
                
                return listOf(Pattern(
                    name = "ML Pattern: ${patternType.name}",
                    type = patternType,
                    timeframe = timeframe,
                    timestamp = ohlc.last().timestamp,
                    confidence = maxConfidence.toDouble(),
                    startIndex = ohlc.size - 10,
                    endIndex = ohlc.size - 1,
                    direction = direction,
                    entryPrice = ohlc.last().close,
                    stopLoss = if (direction == PatternDirection.BULLISH) ohlc.last().low * 0.98 else ohlc.last().high * 1.02,
                    target1 = if (direction == PatternDirection.BULLISH) ohlc.last().close * 1.025 else ohlc.last().close * 0.975,
                    target2 = if (direction == PatternDirection.BULLISH) ohlc.last().close * 1.05 else ohlc.last().close * 0.95,
                    expectedDuration = "3-10 candles",
                    probability = maxConfidence.toDouble(),
                    riskRewardRatio = 2.5
                ))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error running ML pattern detection")
        }
        
        return emptyList()
    }
    
    private fun prepareMLFeatures(ohlc: List<OHLC>): FloatArray {
        val features = mutableListOf<Float>()
        
        // Normalize OHLC values
        val closes = ohlc.map { it.close }
        val minPrice = closes.minOrNull() ?: 0.0
        val maxPrice = closes.maxOrNull() ?: 1.0
        val priceRange = maxPrice - minPrice
        
        for (candle in ohlc) {
            if (priceRange > 0) {
                features.add(((candle.open - minPrice) / priceRange).toFloat())
                features.add(((candle.high - minPrice) / priceRange).toFloat())
                features.add(((candle.low - minPrice) / priceRange).toFloat())
                features.add(((candle.close - minPrice) / priceRange).toFloat())
            } else {
                features.addAll(listOf(0.5f, 0.5f, 0.5f, 0.5f))
            }
        }
        
        // Add technical indicators as features
        val rsi = indicatorEngine.rsi(closes, 14).lastOrNull()?.toFloat() ?: 50f
        features.add(rsi / 100f) // Normalize RSI to 0-1
        
        val macd = indicatorEngine.macd(closes, 12, 26, 9).lastOrNull()
        features.add((macd?.histogram?.toFloat() ?: 0f) / 100f) // Normalize MACD
        
        return features.toFloatArray()
    }
    
    fun generateSignal(patterns: List<Pattern>, ohlc: List<OHLC>, timeframe: String): PatternSignal? {
        if (patterns.isEmpty() || ohlc.isEmpty()) return null
        
        val strongPatterns = patterns.filter { it.confidence > 0.6 }
        if (strongPatterns.isEmpty()) return null
        
        val bullishPatterns = strongPatterns.filter { it.direction == PatternDirection.BULLISH }
        val bearishPatterns = strongPatterns.filter { it.direction == PatternDirection.BEARISH }
        
        val action = when {
            bullishPatterns.size > bearishPatterns.size -> SignalAction.BUY
            bearishPatterns.size > bullishPatterns.size -> SignalAction.SELL
            else -> SignalAction.HOLD
        }
        
        if (action == SignalAction.HOLD) return null
        
        val relevantPatterns = if (action == SignalAction.BUY) bullishPatterns else bearishPatterns
        val avgConfidence = relevantPatterns.map { it.confidence }.average()
        val currentPrice = ohlc.last().close
        
        // Calculate ATR for stop loss and targets
        val atr = indicatorEngine.atr(ohlc.takeLast(20), 14).lastOrNull() ?: (currentPrice * 0.02)
        
        val stopLoss = if (action == SignalAction.BUY) {
            currentPrice - (atr * 2)
        } else {
            currentPrice + (atr * 2)
        }
        
        val target1 = if (action == SignalAction.BUY) {
            currentPrice + (atr * 2)
        } else {
            currentPrice - (atr * 2)
        }
        
        val target2 = if (action == SignalAction.BUY) {
            currentPrice + (atr * 4)
        } else {
            currentPrice - (atr * 4)
        }
        
        val riskAmount = abs(currentPrice - stopLoss)
        val reward1 = abs(target1 - currentPrice)
        val riskRewardRatio = if (riskAmount > 0) reward1 / riskAmount else 2.0
        
        return PatternSignal(
            action = action,
            confidence = avgConfidence,
            entryPrice = currentPrice,
            stopLoss = stopLoss,
            target1 = target1,
            target2 = target2,
            timeframe = timeframe,
            expectedDuration = relevantPatterns.firstOrNull()?.expectedDuration ?: "3-8 candles",
            probability = avgConfidence,
            positionSize = 0.02, // 2% risk per trade
            riskRewardRatio = riskRewardRatio,
            reasoning = "Detected ${relevantPatterns.size} strong ${action.name.lowercase()} patterns: ${relevantPatterns.joinToString(", ") { it.name }}"
        )
    }
}