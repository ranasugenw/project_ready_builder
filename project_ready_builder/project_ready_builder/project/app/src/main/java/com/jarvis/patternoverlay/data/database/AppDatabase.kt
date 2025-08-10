package com.jarvis.patternoverlay.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.jarvis.patternoverlay.data.dao.PatternDao
import com.jarvis.patternoverlay.model.Pattern

@Database(
    entities = [Pattern::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patternDao(): PatternDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jarvis_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}