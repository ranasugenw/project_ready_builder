package com.jarvis.patternoverlay.engine

import com.jarvis.patternoverlay.model.OHLC
import kotlin.math.*

class IndicatorEngine {
    
    // Moving Averages
    fun sma(prices: List<Double>, period: Int): List<Double> {
        if (prices.size < period) return emptyList()
        
        val result = mutableListOf<Double>()
        for (i in period - 1 until prices.size) {
            val sum = prices.subList(i - period + 1, i + 1).sum()
            result.add(sum / period)
        }
        return result
    }
    
    fun ema(prices: List<Double>, period: Int): List<Double> {
        if (prices.size < period) return emptyList()
        
        val multiplier = 2.0 / (period + 1)
        val result = mutableListOf<Double>()
        
        // Start with SMA for first value
        val initialSMA = prices.take(period).sum() / period
        result.add(initialSMA)
        
        for (i in period until prices.size) {
            val ema = (prices[i] * multiplier) + (result.last() * (1 - multiplier))
            result.add(ema)
        }
        
        return result
    }
    
    // RSI
    fun rsi(prices: List<Double>, period: Int = 14): List<Double> {
        if (prices.size < period + 1) return emptyList()
        
        val changes = mutableListOf<Double>()
        for (i in 1 until prices.size) {
            changes.add(prices[i] - prices[i - 1])
        }
        
        val gains = changes.map { if (it > 0) it else 0.0 }
        val losses = changes.map { if (it < 0) abs(it) else 0.0 }
        
        val avgGains = ema(gains, period)
        val avgLosses = ema(losses, period)
        
        val result = mutableListOf<Double>()
        for (i in avgGains.indices) {
            val rs = if (avgLosses[i] != 0.0) avgGains[i] / avgLosses[i] else 100.0
            val rsi = 100.0 - (100.0 / (1.0 + rs))
            result.add(rsi)
        }
        
        return result
    }
    
    // MACD
    data class MACDResult(val macd: Double, val signal: Double, val histogram: Double)
    
    fun macd(prices: List<Double>, fastPeriod: Int = 12, slowPeriod: Int = 26, signalPeriod: Int = 9): List<MACDResult> {
        val fastEMA = ema(prices, fastPeriod)
        val slowEMA = ema(prices, slowPeriod)
        
        if (fastEMA.size != slowEMA.size) return emptyList()
        
        val macdLine = mutableListOf<Double>()
        for (i in fastEMA.indices) {
            macdLine.add(fastEMA[i] - slowEMA[i])
        }
        
        val signalLine = ema(macdLine, signalPeriod)
        val result = mutableListOf<MACDResult>()
        
        val startIndex = macdLine.size - signalLine.size
        for (i in signalLine.indices) {
            val macdValue = macdLine[startIndex + i]
            val signalValue = signalLine[i]
            val histogram = macdValue - signalValue
            result.add(MACDResult(macdValue, signalValue, histogram))
        }
        
        return result
    }
    
    // Bollinger Bands
    data class BollingerBands(val upper: Double, val middle: Double, val lower: Double)
    
    fun bollingerBands(prices: List<Double>, period: Int = 20, stdDev: Double = 2.0): List<BollingerBands> {
        val smaValues = sma(prices, period)
        val result = mutableListOf<BollingerBands>()
        
        for (i in smaValues.indices) {
            val dataIndex = i + period - 1
            val subset = prices.subList(dataIndex - period + 1, dataIndex + 1)
            val mean = smaValues[i]
            val variance = subset.map { (it - mean).pow(2) }.sum() / period
            val standardDeviation = sqrt(variance)
            
            result.add(BollingerBands(
                upper = mean + (stdDev * standardDeviation),
                middle = mean,
                lower = mean - (stdDev * standardDeviation)
            ))
        }
        
        return result
    }
    
    // ATR (Average True Range)
    fun atr(ohlc: List<OHLC>, period: Int = 14): List<Double> {
        if (ohlc.size < 2) return emptyList()
        
        val trueRanges = mutableListOf<Double>()
        
        for (i in 1 until ohlc.size) {
            val current = ohlc[i]
            val previous = ohlc[i - 1]
            
            val tr1 = current.high - current.low
            val tr2 = abs(current.high - previous.close)
            val tr3 = abs(current.low - previous.close)
            
            trueRanges.add(maxOf(tr1, tr2, tr3))
        }
        
        return sma(trueRanges, period)
    }
    
    // Stochastic Oscillator
    data class StochasticResult(val k: Double, val d: Double)
    
    fun stochastic(ohlc: List<OHLC>, kPeriod: Int = 14, dPeriod: Int = 3): List<StochasticResult> {
        if (ohlc.size < kPeriod) return emptyList()
        
        val kValues = mutableListOf<Double>()
        
        for (i in kPeriod - 1 until ohlc.size) {
            val subset = ohlc.subList(i - kPeriod + 1, i + 1)
            val highestHigh = subset.maxOf { it.high }
            val lowestLow = subset.minOf { it.low }
            val currentClose = ohlc[i].close
            
            val k = if (highestHigh != lowestLow) {
                ((currentClose - lowestLow) / (highestHigh - lowestLow)) * 100
            } else {
                50.0
            }
            
            kValues.add(k)
        }
        
        val dValues = sma(kValues, dPeriod)
        val result = mutableListOf<StochasticResult>()
        
        val startIndex = kValues.size - dValues.size
        for (i in dValues.indices) {
            result.add(StochasticResult(
                k = kValues[startIndex + i],
                d = dValues[i]
            ))
        }
        
        return result
    }
    
    // ADX (Average Directional Index)
    data class ADXResult(val adx: Double, val plusDI: Double, val minusDI: Double)
    
    fun adx(ohlc: List<OHLC>, period: Int = 14): List<ADXResult> {
        if (ohlc.size < period + 1) return emptyList()
        
        val plusDM = mutableListOf<Double>()
        val minusDM = mutableListOf<Double>()
        val trueRanges = mutableListOf<Double>()
        
        for (i in 1 until ohlc.size) {
            val current = ohlc[i]
            val previous = ohlc[i - 1]
            
            val upMove = current.high - previous.high
            val downMove = previous.low - current.low
            
            plusDM.add(if (upMove > downMove && upMove > 0) upMove else 0.0)
            minusDM.add(if (downMove > upMove && downMove > 0) downMove else 0.0)
            
            val tr1 = current.high - current.low
            val tr2 = abs(current.high - previous.close)
            val tr3 = abs(current.low - previous.close)
            trueRanges.add(maxOf(tr1, tr2, tr3))
        }
        
        val smoothedPlusDM = ema(plusDM, period)
        val smoothedMinusDM = ema(minusDM, period)
        val smoothedTR = ema(trueRanges, period)
        
        val result = mutableListOf<ADXResult>()
        val dxValues = mutableListOf<Double>()
        
        for (i in smoothedTR.indices) {
            val plusDI = (smoothedPlusDM[i] / smoothedTR[i]) * 100
            val minusDI = (smoothedMinusDM[i] / smoothedTR[i]) * 100
            
            val dx = if (plusDI + minusDI != 0.0) {
                (abs(plusDI - minusDI) / (plusDI + minusDI)) * 100
            } else {
                0.0
            }
            
            dxValues.add(dx)
            
            if (dxValues.size >= period) {
                val adxValue = dxValues.takeLast(period).average()
                result.add(ADXResult(adxValue, plusDI, minusDI))
            }
        }
        
        return result
    }
    
    // On Balance Volume
    fun obv(ohlc: List<OHLC>): List<Double> {
        if (ohlc.isEmpty()) return emptyList()
        
        val result = mutableListOf<Double>()
        var currentOBV = 0.0
        result.add(currentOBV)
        
        for (i in 1 until ohlc.size) {
            val current = ohlc[i]
            val previous = ohlc[i - 1]
            
            when {
                current.close > previous.close -> currentOBV += current.volume
                current.close < previous.close -> currentOBV -= current.volume
                // If close prices are equal, OBV remains unchanged
            }
            
            result.add(currentOBV)
        }
        
        return result
    }
    
    // VWAP (Volume Weighted Average Price)
    fun vwap(ohlc: List<OHLC>): List<Double> {
        val result = mutableListOf<Double>()
        var cumulativeVolumePrice = 0.0
        var cumulativeVolume = 0.0
        
        for (candle in ohlc) {
            val typicalPrice = (candle.high + candle.low + candle.close) / 3
            cumulativeVolumePrice += typicalPrice * candle.volume
            cumulativeVolume += candle.volume
            
            val vwap = if (cumulativeVolume > 0) {
                cumulativeVolumePrice / cumulativeVolume
            } else {
                typicalPrice
            }
            
            result.add(vwap)
        }
        
        return result
    }
    
    // Commodity Channel Index
    fun cci(ohlc: List<OHLC>, period: Int = 20): List<Double> {
        if (ohlc.size < period) return emptyList()
        
        val typicalPrices = ohlc.map { (it.high + it.low + it.close) / 3 }
        val smaTypical = sma(typicalPrices, period)
        val result = mutableListOf<Double>()
        
        for (i in smaTypical.indices) {
            val dataIndex = i + period - 1
            val currentTypical = typicalPrices[dataIndex]
            val smaValue = smaTypical[i]
            
            // Calculate mean deviation
            val subset = typicalPrices.subList(dataIndex - period + 1, dataIndex + 1)
            val meanDeviation = subset.map { abs(it - smaValue) }.sum() / period
            
            val cci = if (meanDeviation != 0.0) {
                (currentTypical - smaValue) / (0.015 * meanDeviation)
            } else {
                0.0
            }
            
            result.add(cci)
        }
        
        return result
    }
    
    // Williams %R
    fun williamsR(ohlc: List<OHLC>, period: Int = 14): List<Double> {
        if (ohlc.size < period) return emptyList()
        
        val result = mutableListOf<Double>()
        
        for (i in period - 1 until ohlc.size) {
            val subset = ohlc.subList(i - period + 1, i + 1)
            val highestHigh = subset.maxOf { it.high }
            val lowestLow = subset.minOf { it.low }
            val currentClose = ohlc[i].close
            
            val williamsR = if (highestHigh != lowestLow) {
                ((highestHigh - currentClose) / (highestHigh - lowestLow)) * -100
            } else {
                -50.0
            }
            
            result.add(williamsR)
        }
        
        return result
    }
}