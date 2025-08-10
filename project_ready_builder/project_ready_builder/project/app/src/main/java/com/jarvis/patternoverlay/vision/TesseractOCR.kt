package com.jarvis.patternoverlay.vision

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TesseractOCR(private val context: Context) {
    
    private var tessBaseAPI: TessBaseAPI? = null
    private var isInitialized = false
    
    init {
        initializeTesseract()
    }
    
    private fun initializeTesseract() {
        try {
            val dataPath = File(context.filesDir, "tesseract")
            if (!dataPath.exists()) {
                dataPath.mkdirs()
            }
            
            val tessDataPath = File(dataPath, "tessdata")
            if (!tessDataPath.exists()) {
                tessDataPath.mkdirs()
            }
            
            // Copy trained data file from assets
            val trainedDataFile = File(tessDataPath, "eng.traineddata")
            if (!trainedDataFile.exists()) {
                copyAssetToFile("tesseract/eng.traineddata", trainedDataFile)
            }
            
            tessBaseAPI = TessBaseAPI().apply {
                init(dataPath.absolutePath, "eng")
                setVariable("tessedit_char_whitelist", "0123456789.,")
                setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK)
            }
            
            isInitialized = true
            Timber.d("Tesseract initialized successfully")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Tesseract")
            isInitialized = false
        }
    }
    
    fun extractPriceLabels(bitmap: Bitmap): List<Double> {
        if (!isInitialized || tessBaseAPI == null) {
            return emptyList()
        }
        
        return try {
            tessBaseAPI?.setImage(bitmap)
            val text = tessBaseAPI?.utF8Text ?: ""
            
            // Parse price values from extracted text
            val prices = mutableListOf<Double>()
            val lines = text.split('\n')
            
            for (line in lines) {
                val cleanLine = line.trim().replace(",", "")
                try {
                    val price = cleanLine.toDouble()
                    if (price > 0) {
                        prices.add(price)
                    }
                } catch (e: NumberFormatException) {
                    // Skip invalid numbers
                }
            }
            
            prices
        } catch (e: Exception) {
            Timber.e(e, "Error extracting price labels")
            emptyList()
        }
    }
    
    private fun copyAssetToFile(assetPath: String, outputFile: File) {
        try {
            context.assets.open(assetPath).use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: IOException) {
            Timber.e(e, "Failed to copy asset: $assetPath")
            throw e
        }
    }
    
    fun cleanup() {
        tessBaseAPI?.end()
        tessBaseAPI = null
        isInitialized = false
    }
}