package com.jarvis.patternoverlay.data.dao

import androidx.room.*
import com.jarvis.patternoverlay.model.Pattern
import kotlinx.coroutines.flow.Flow

@Dao
interface PatternDao {
    
    @Query("SELECT * FROM patterns ORDER BY timestamp DESC LIMIT 100")
    fun getAllPatterns(): Flow<List<Pattern>>
    
    @Query("SELECT * FROM patterns WHERE timeframe = :timeframe ORDER BY timestamp DESC LIMIT 50")
    fun getPatternsByTimeframe(timeframe: String): Flow<List<Pattern>>
    
    @Query("SELECT * FROM patterns WHERE confidence >= :minConfidence ORDER BY timestamp DESC")
    fun getHighConfidencePatterns(minConfidence: Double): Flow<List<Pattern>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: Pattern): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatterns(patterns: List<Pattern>)
    
    @Update
    suspend fun updatePattern(pattern: Pattern)
    
    @Delete
    suspend fun deletePattern(pattern: Pattern)
    
    @Query("DELETE FROM patterns WHERE timestamp < :cutoffTime")
    suspend fun deleteOldPatterns(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM patterns")
    suspend fun getPatternCount(): Int
    
    @Query("SELECT * FROM patterns WHERE name LIKE :patternName ORDER BY timestamp DESC LIMIT 20")
    fun searchPatterns(patternName: String): Flow<List<Pattern>>
    
    @Query("SELECT DISTINCT timeframe FROM patterns ORDER BY timeframe")
    suspend fun getAllTimeframes(): List<String>
}