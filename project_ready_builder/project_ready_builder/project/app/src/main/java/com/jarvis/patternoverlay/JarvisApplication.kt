package com.jarvis.patternoverlay

import android.app.Application
import androidx.room.Room
import com.jarvis.patternoverlay.data.database.AppDatabase
import com.jarvis.patternoverlay.data.repository.PatternRepository
import com.jarvis.patternoverlay.engine.IndicatorEngine
import com.jarvis.patternoverlay.engine.PatternEngine
import com.jarvis.patternoverlay.vision.ImageProcessor
import org.opencv.android.OpenCVLoaderCallback
import org.opencv.android.OpenCVManagerService
import org.opencv.android.InstallCallbackInterface
import timber.log.Timber

class JarvisApplication : Application() {
    
    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "jarvis_database"
        ).build()
    }
    
    val patternRepository by lazy {
        PatternRepository(database.patternDao())
    }
    
    val indicatorEngine by lazy {
        IndicatorEngine()
    }
    
    val patternEngine by lazy {
        PatternEngine(applicationContext)
    }
    
    val imageProcessor by lazy {
        ImageProcessor(applicationContext)
    }
    
    private val openCVLoaderCallback = object : OpenCVLoaderCallback() {
        override fun onManagerConnected(status: Int) {
            when (status) {
                OpenCVLoaderCallback.SUCCESS -> {
                    Timber.d("OpenCV loaded successfully")
                    imageProcessor.initializeOpenCV()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
        
        override fun onPackageInstall(operation: Int, callback: InstallCallbackInterface?) {
            super.onPackageInstall(operation, callback)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Initialize OpenCV
        if (!org.opencv.android.OpenCVLoader.initDebug()) {
            Timber.d("Internal OpenCV library not found. Using OpenCV Manager for initialization")
            org.opencv.android.OpenCVLoader.initAsync(org.opencv.android.OpenCVLoader.OPENCV_VERSION, this, openCVLoaderCallback)
        } else {
            Timber.d("OpenCV library found inside package. Using it!")
            openCVLoaderCallback.onManagerConnected(OpenCVLoaderCallback.SUCCESS)
        }
    }
}