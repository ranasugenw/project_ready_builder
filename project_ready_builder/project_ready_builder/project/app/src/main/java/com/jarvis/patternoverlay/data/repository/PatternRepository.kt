package com.jarvis.patternoverlay.data.repository

import com.jarvis.patternoverlay.data.dao.PatternDao
import com.jarvis.patternoverlay.model.Pattern
import kotlinx.coroutines.flow.Flow

class PatternRepository(private val patternDao: PatternDao) {
    
    fun getAllPatterns(): Flow<List<Pattern>> {
        return patternDao.getAllPatterns()
    }
    
    fun getPatternsByTimeframe(timeframe: String): Flow<List<Pattern>> {
        return patternDao.getPatternsByTimeframe(timeframe)
    }
    
    fun getHighConfidencePatterns(minConfidence: Double): Flow<List<Pattern>> {
        return patternDao.getHighConfidencePatterns(minConfidence)
    }
    
    suspend fun insertPattern(pattern: Pattern): Long {
        return patternDao.insertPattern(pattern)
    }
    
    suspend fun insertPatterns(patterns: List<Pattern>) {
        patternDao.insertPatterns(patterns)
    }
    
    suspend fun updatePattern(pattern: Pattern) {
        patternDao.updatePattern(pattern)
    }
    
    suspend fun deletePattern(pattern: Pattern) {
        patternDao.deletePattern(pattern)
    }
    
    suspend fun deleteOldPatterns(cutoffTime: Long) {
        patternDao.deleteOldPatterns(cutoffTime)
    }
    
    suspend fun getPatternCount(): Int {
        return patternDao.getPatternCount()
    }
    
    fun searchPatterns(patternName: String): Flow<List<Pattern>> {
        return patternDao.searchPatterns("%$patternName%")
    }
    
    suspend fun getAllTimeframes(): List<String> {
        return patternDao.getAllTimeframes()
    }
}