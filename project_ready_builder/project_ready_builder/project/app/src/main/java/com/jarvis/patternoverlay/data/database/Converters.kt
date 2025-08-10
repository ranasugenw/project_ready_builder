package com.jarvis.patternoverlay.data.database

import androidx.room.TypeConverter
import com.jarvis.patternoverlay.model.PatternDirection
import com.jarvis.patternoverlay.model.PatternType

class Converters {
    
    @TypeConverter
    fun fromPatternType(type: PatternType): String {
        return type.name
    }
    
    @TypeConverter
    fun toPatternType(typeName: String): PatternType {
        return PatternType.valueOf(typeName)
    }
    
    @TypeConverter
    fun fromPatternDirection(direction: PatternDirection): String {
        return direction.name
    }
    
    @TypeConverter
    fun toPatternDirection(directionName: String): PatternDirection {
        return PatternDirection.valueOf(directionName)
    }
}