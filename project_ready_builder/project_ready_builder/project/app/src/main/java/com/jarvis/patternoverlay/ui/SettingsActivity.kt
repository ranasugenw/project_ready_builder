package com.jarvis.patternoverlay.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.patternoverlay.R
import com.jarvis.patternoverlay.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        loadSettings()
        setupListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
    }
    
    private fun loadSettings() {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        
        // Load current settings
        binding.spinnerTargetApp.setSelection(prefs.getInt("target_app", 0))
        binding.switchDarkTheme.isChecked = prefs.getBoolean("dark_theme", true)
        binding.seekBarFPS.progress = prefs.getInt("fps", 2) - 1
        binding.seekBarConfidence.progress = (prefs.getFloat("min_confidence", 0.6f) * 100).toInt()
        binding.seekBarRsiPeriod.progress = prefs.getInt("rsi_period", 14) - 5
        
        updateFPSLabel(prefs.getInt("fps", 2))
        updateConfidenceLabel(prefs.getFloat("min_confidence", 0.6f))
        updateRSILabel(prefs.getInt("rsi_period", 14))
    }
    
    private fun setupListeners() {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        
        binding.spinnerTargetApp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                prefs.edit().putInt("target_app", position).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        binding.switchDarkTheme.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_theme", isChecked).apply()
        }
        
        binding.seekBarFPS.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fps = progress + 1
                updateFPSLabel(fps)
                if (fromUser) {
                    prefs.edit().putInt("fps", fps).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        binding.seekBarConfidence.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val confidence = progress / 100f
                updateConfidenceLabel(confidence)
                if (fromUser) {
                    prefs.edit().putFloat("min_confidence", confidence).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        binding.seekBarRsiPeriod.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val period = progress + 5
                updateRSILabel(period)
                if (fromUser) {
                    prefs.edit().putInt("rsi_period", period).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        binding.btnResetSettings.setOnClickListener {
            resetToDefaults()
        }
    }
    
    private fun updateFPSLabel(fps: Int) {
        binding.labelFPS.text = "FPS: $fps"
    }
    
    private fun updateConfidenceLabel(confidence: Float) {
        binding.labelConfidence.text = "Min Confidence: ${(confidence * 100).toInt()}%"
    }
    
    private fun updateRSILabel(period: Int) {
        binding.labelRSI.text = "RSI Period: $period"
    }
    
    private fun resetToDefaults() {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        prefs.edit().clear().apply()
        loadSettings()
        Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}