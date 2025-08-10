package com.jarvis.patternoverlay.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patterns")
data class Pattern(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: PatternType,
    val timeframe: String,
    val timestamp: Long,
    val confidence: Double,
    val startIndex: Int,
    val endIndex: Int,
    val direction: PatternDirection,
    val entryPrice: Double,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val expectedDuration: String,
    val probability: Double,
    val riskRewardRatio: Double
)

enum class PatternType {
    // Candlestick Patterns
    DOJI, HAMMER, SHOOTING_STAR, ENGULFING_BULLISH, ENGULFING_BEARISH,
    MORNING_STAR, EVENING_STAR, DARK_CLOUD_COVER, PIERCING_LINE,
    THREE_WHITE_SOLDIERS, THREE_BLACK_CROWS, SPINNING_TOP,
    HANGING_MAN, INVERTED_HAMMER, HARAMI_BULLISH, HARAMI_BEARISH,
    
    // Chart Patterns
    DOUBLE_TOP, DOUBLE_BOTTOM, TRIPLE_TOP, TRIPLE_BOTTOM,
    HEAD_AND_SHOULDERS, INVERSE_HEAD_AND_SHOULDERS,
    ASCENDING_TRIANGLE, DESCENDING_TRIANGLE, SYMMETRICAL_TRIANGLE,
    WEDGE_RISING, WEDGE_FALLING, FLAG_BULLISH, FLAG_BEARISH,
    PENNANT_BULLISH, PENNANT_BEARISH, CHANNEL_UP, CHANNEL_DOWN,
    CUP_AND_HANDLE, RECTANGLE, GAP_UP, GAP_DOWN,
    
    // Volume Patterns
    VOLUME_SPIKE, VOLUME_DIVERGENCE, ON_BALANCE_VOLUME_BREAKOUT,
    
    // ML Detected
    ML_BULLISH, ML_BEARISH, ML_CONTINUATION, ML_REVERSAL
}

enum class PatternDirection {
    BULLISH, BEARISH, NEUTRAL
}

data class PatternSignal(
    val action: SignalAction,
    val confidence: Double,
    val entryPrice: Double,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val timeframe: String,
    val expectedDuration: String,
    val probability: Double,
    val positionSize: Double,
    val riskRewardRatio: Double,
    val reasoning: String
)

enum class SignalAction {
    BUY, SELL, HOLD
}