package com.jarvis.patternoverlay.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OHLC(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double = 0.0
) : Parcelable {
    
    val bodySize: Double
        get() = kotlin.math.abs(close - open)
    
    val upperWick: Double
        get() = high - kotlin.math.max(open, close)
    
    val lowerWick: Double
        get() = kotlin.math.min(open, close) - low
    
    val totalRange: Double
        get() = high - low
    
    val isBullish: Boolean
        get() = close > open
    
    val isBearish: Boolean
        get() = close < open
    
    val isDoji: Boolean
        get() = bodySize < (totalRange * 0.1)
    
    companion object {
        fun fromPixelData(
            timestamp: Long,
            candleTop: Float,
            candleBottom: Float,
            wickTop: Float,
            wickBottom: Float,
            openPixel: Float,
            closePixel: Float,
            priceScale: PriceScale,
            volume: Double = 0.0
        ): OHLC {
            val high = priceScale.pixelToPrice(wickTop)
            val low = priceScale.pixelToPrice(wickBottom)
            val open = priceScale.pixelToPrice(openPixel)
            val close = priceScale.pixelToPrice(closePixel)
            
            return OHLC(timestamp, open, high, low, close, volume)
        }
    }
}

@Parcelize
data class PriceScale(
    val minPrice: Double,
    val maxPrice: Double,
    val topPixel: Float,
    val bottomPixel: Float
) : Parcelable {
    
    val pixelsPerUnit: Double
        get() = (bottomPixel - topPixel) / (maxPrice - minPrice)
    
    fun pixelToPrice(pixel: Float): Double {
        val normalizedPixel = (pixel - topPixel) / (bottomPixel - topPixel)
        return maxPrice - (normalizedPixel * (maxPrice - minPrice))
    }
    
    fun priceToPixel(price: Double): Float {
        val normalizedPrice = (maxPrice - price) / (maxPrice - minPrice)
        return topPixel + (normalizedPrice * (bottomPixel - topPixel)).toFloat()
    }
}