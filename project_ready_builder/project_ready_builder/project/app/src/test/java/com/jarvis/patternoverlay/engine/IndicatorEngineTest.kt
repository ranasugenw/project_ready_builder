package com.jarvis.patternoverlay.engine

import com.jarvis.patternoverlay.model.OHLC
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class IndicatorEngineTest {
    
    private lateinit var indicatorEngine: IndicatorEngine
    private lateinit var samplePrices: List<Double>
    private lateinit var sampleOHLC: List<OHLC>
    
    @Before
    fun setup() {
        indicatorEngine = IndicatorEngine()
        
        // Sample price data for testing
        samplePrices = listOf(
            44.0, 44.3, 44.1, 44.2, 44.5, 43.9, 44.9, 44.5, 44.6, 44.8,
            44.2, 44.6, 44.8, 44.2, 44.6, 44.0, 44.2, 44.5, 44.9, 44.5,
            44.6, 44.8, 44.2, 44.6, 44.0, 44.2, 44.5, 44.9, 44.5, 44.6
        )
        
        // Sample OHLC data
        sampleOHLC = samplePrices.mapIndexed { index, close ->
            val timestamp = 1640995200000L + (index * 60000L) // 1 minute intervals
            val open = if (index > 0) samplePrices[index - 1] else close
            val high = close + 0.2
            val low = close - 0.2
            OHLC(timestamp, open, high, low, close, 1000.0)
        }
    }
    
    @Test
    fun testSMA() {
        val result = indicatorEngine.sma(samplePrices, 5)
        assertNotNull(result)
        assertTrue("SMA should return values", result.isNotEmpty())
        assertEquals("SMA should have correct number of values", samplePrices.size - 4, result.size)
        
        // Test first SMA value
        val expectedFirst = samplePrices.take(5).average()
        assertEquals("First SMA value should be correct", expectedFirst, result[0], 0.001)
    }
    
    @Test
    fun testEMA() {
        val result = indicatorEngine.ema(samplePrices, 10)
        assertNotNull(result)
        assertTrue("EMA should return values", result.isNotEmpty())
        assertEquals("EMA should have correct number of values", samplePrices.size - 9, result.size)
        
        // EMA values should be different from SMA
        val smaResult = indicatorEngine.sma(samplePrices, 10)
        assertNotEquals("EMA should differ from SMA", smaResult.last(), result.last(), 0.1)
    }
    
    @Test
    fun testRSI() {
        val result = indicatorEngine.rsi(samplePrices, 14)
        assertNotNull(result)
        assertTrue("RSI should return values", result.isNotEmpty())
        
        // RSI should be between 0 and 100
        for (rsi in result) {
            assertTrue("RSI should be >= 0", rsi >= 0.0)
            assertTrue("RSI should be <= 100", rsi <= 100.0)
        }
    }
    
    @Test
    fun testMACD() {
        val result = indicatorEngine.macd(samplePrices, 12, 26, 9)
        assertNotNull(result)
        assertTrue("MACD should return values", result.isNotEmpty())
        
        // Check that histogram is the difference between MACD and signal
        val lastResult = result.last()
        val expectedHistogram = lastResult.macd - lastResult.signal
        assertEquals("Histogram should be MACD - Signal", expectedHistogram, lastResult.histogram, 0.001)
    }
    
    @Test
    fun testBollingerBands() {
        val result = indicatorEngine.bollingerBands(samplePrices, 20, 2.0)
        assertNotNull(result)
        assertTrue("Bollinger Bands should return values", result.isNotEmpty())
        
        for (bb in result) {
            assertTrue("Upper band should be > middle", bb.upper > bb.middle)
            assertTrue("Middle should be > lower band", bb.middle > bb.lower)
        }
    }
    
    @Test
    fun testATR() {
        val result = indicatorEngine.atr(sampleOHLC, 14)
        assertNotNull(result)
        assertTrue("ATR should return values", result.isNotEmpty())
        
        // ATR should always be positive
        for (atr in result) {
            assertTrue("ATR should be positive", atr > 0.0)
        }
    }
    
    @Test
    fun testStochastic() {
        val result = indicatorEngine.stochastic(sampleOHLC, 14, 3)
        assertNotNull(result)
        assertTrue("Stochastic should return values", result.isNotEmpty())
        
        for (stoch in result) {
            assertTrue("Stochastic %K should be between 0-100", stoch.k >= 0.0 && stoch.k <= 100.0)
            assertTrue("Stochastic %D should be between 0-100", stoch.d >= 0.0 && stoch.d <= 100.0)
        }
    }
    
    @Test
    fun testADX() {
        val result = indicatorEngine.adx(sampleOHLC, 14)
        assertNotNull(result)
        assertTrue("ADX should return values", result.isNotEmpty())
        
        for (adxResult in result) {
            assertTrue("ADX should be positive", adxResult.adx >= 0.0)
            assertTrue("ADX should be <= 100", adxResult.adx <= 100.0)
            assertTrue("+DI should be positive", adxResult.plusDI >= 0.0)
            assertTrue("-DI should be positive", adxResult.minusDI >= 0.0)
        }
    }
    
    @Test
    fun testOBV() {
        val result = indicatorEngine.obv(sampleOHLC)
        assertNotNull(result)
        assertEquals("OBV should have same length as input", sampleOHLC.size, result.size)
        
        // First OBV value should be 0
        assertEquals("First OBV value should be 0", 0.0, result[0], 0.001)
    }
    
    @Test
    fun testVWAP() {
        val result = indicatorEngine.vwap(sampleOHLC)
        assertNotNull(result)
        assertEquals("VWAP should have same length as input", sampleOHLC.size, result.size)
        
        // VWAP values should be positive
        for (vwap in result) {
            assertTrue("VWAP should be positive", vwap > 0.0)
        }
    }
    
    @Test
    fun testCCI() {
        val result = indicatorEngine.cci(sampleOHLC, 20)
        assertNotNull(result)
        assertTrue("CCI should return values", result.isNotEmpty())
        
        // CCI can be any value, but typically ranges from -300 to +300
        for (cci in result) {
            assertTrue("CCI should be reasonable", cci >= -500.0 && cci <= 500.0)
        }
    }
    
    @Test
    fun testWilliamsR() {
        val result = indicatorEngine.williamsR(sampleOHLC, 14)
        assertNotNull(result)
        assertTrue("Williams %R should return values", result.isNotEmpty())
        
        for (wr in result) {
            assertTrue("Williams %R should be between -100 and 0", wr >= -100.0 && wr <= 0.0)
        }
    }
    
    @Test
    fun testEmptyInput() {
        val emptyPrices = emptyList<Double>()
        val emptyOHLC = emptyList<OHLC>()
        
        assertTrue("SMA with empty input should return empty", indicatorEngine.sma(emptyPrices, 5).isEmpty())
        assertTrue("RSI with empty input should return empty", indicatorEngine.rsi(emptyPrices, 14).isEmpty())
        assertTrue("ATR with empty input should return empty", indicatorEngine.atr(emptyOHLC, 14).isEmpty())
    }
    
    @Test
    fun testInsufficientData() {
        val shortPrices = listOf(44.0, 44.1, 44.2)
        val shortOHLC = shortPrices.mapIndexed { index, close ->
            OHLC(index.toLong(), close, close + 0.1, close - 0.1, close, 1000.0)
        }
        
        assertTrue("SMA with insufficient data should return empty", indicatorEngine.sma(shortPrices, 10).isEmpty())
        assertTrue("ATR with insufficient data should return empty", indicatorEngine.atr(shortOHLC, 14).isEmpty())
    }
}