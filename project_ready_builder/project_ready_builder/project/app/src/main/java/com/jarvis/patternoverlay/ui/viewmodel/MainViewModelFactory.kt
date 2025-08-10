package com.jarvis.patternoverlay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jarvis.patternoverlay.data.repository.PatternRepository
import com.jarvis.patternoverlay.engine.IndicatorEngine
import com.jarvis.patternoverlay.engine.PatternEngine
import com.jarvis.patternoverlay.vision.ImageProcessor

class MainViewModelFactory(
    private val patternRepository: PatternRepository,
    private val indicatorEngine: IndicatorEngine,
    private val patternEngine: PatternEngine,
    private val imageProcessor: ImageProcessor
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                patternRepository,
                indicatorEngine,
                patternEngine,
                imageProcessor
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}