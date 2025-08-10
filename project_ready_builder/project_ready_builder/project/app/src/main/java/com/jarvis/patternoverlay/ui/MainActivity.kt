package com.jarvis.patternoverlay.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.jarvis.patternoverlay.JarvisApplication
import com.jarvis.patternoverlay.R
import com.jarvis.patternoverlay.databinding.ActivityMainBinding
import com.jarvis.patternoverlay.service.OverlayService
import com.jarvis.patternoverlay.service.PatternDetectionService
import com.jarvis.patternoverlay.service.ScreenCaptureService
import com.jarvis.patternoverlay.ui.viewmodel.MainViewModel
import com.jarvis.patternoverlay.ui.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    
    private var screenCaptureService: ScreenCaptureService? = null
    private var isServiceBound = false
    
    private lateinit var mediaProjectionLauncher: ActivityResultLauncher<Intent>
    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ScreenCaptureService.LocalBinder
            screenCaptureService = binder.getService()
            isServiceBound = true
            
            // Set up capture listener
            screenCaptureService?.setCaptureListener { bitmap ->
                viewModel.processCapturedScreen(bitmap)
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            screenCaptureService = null
            isServiceBound = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupWindowInsets()
        setupViewModel()
        setupUI()
        setupActivityLaunchers()
        checkFirstLaunch()
    }
    
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    
    private fun setupViewModel() {
        val app = application as JarvisApplication
        val factory = MainViewModelFactory(app.patternRepository, app.indicatorEngine, app.patternEngine, app.imageProcessor)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
        
        // Observe patterns
        viewModel.detectedPatterns.observe(this) { patterns ->
            updatePatternsList(patterns)
        }
        
        // Observe signals
        viewModel.currentSignal.observe(this) { signal ->
            updateSignalDisplay(signal)
            
            // Send to overlay service
            val intent = Intent(this, OverlayService::class.java).apply {
                action = "UPDATE_PATTERNS"
                putParcelableArrayListExtra("patterns", ArrayList(viewModel.detectedPatterns.value ?: emptyList()))
                putExtra("signal", signal)
            }
            startService(intent)
        }
        
        // Observe status
        viewModel.isAnalyzing.observe(this) { isAnalyzing ->
            binding.btnStartStop.text = if (isAnalyzing) "Stop Analysis" else "Start Analysis"
            binding.statusIndicator.setBackgroundColor(
                if (isAnalyzing) getColor(R.color.status_active) else getColor(R.color.status_inactive)
            )
        }
    }
    
    private fun setupUI() {
        binding.btnStartStop.setOnClickListener {
            if (viewModel.isAnalyzing.value == true) {
                stopAnalysis()
            } else {
                startAnalysis()
            }
        }
        
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        binding.btnCalibrate.setOnClickListener {
            startActivity(Intent(this, CalibrationActivity::class.java))
        }
        
        binding.btnBacktest.setOnClickListener {
            // TODO: Implement backtest activity
            Toast.makeText(this, "Backtest feature coming soon", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnJournal.setOnClickListener {
            // TODO: Implement journal activity
            Toast.makeText(this, "Trading journal coming soon", Toast.LENGTH_SHORT).show()
        }
        
        // Initially show welcome message
        updateStatusText("Welcome to Jarvis Pattern Overlay")
    }
    
    private fun setupActivityLaunchers() {
        mediaProjectionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                startScreenCapture(result.resultCode, result.data!!)
            } else {
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
            }
        }
        
        overlayPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { _ ->
            if (Settings.canDrawOverlays(this)) {
                requestScreenCapturePermission()
            } else {
                Toast.makeText(this, "Overlay permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun checkFirstLaunch() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("onboarding_completed", false)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }
    }
    
    private fun startAnalysis() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }
        
        requestScreenCapturePermission()
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }
    
    private fun requestScreenCapturePermission() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(intent)
    }
    
    private fun startScreenCapture(resultCode: Int, data: Intent) {
        // Bind to screen capture service
        val serviceIntent = Intent(this, ScreenCaptureService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        // Start the service
        serviceIntent.apply {
            action = ScreenCaptureService.ACTION_START_CAPTURE
            putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenCaptureService.EXTRA_DATA, data)
        }
        startForegroundService(serviceIntent)
        
        // Start pattern detection service
        startService(Intent(this, PatternDetectionService::class.java))
        
        // Start overlay service
        startService(Intent(this, OverlayService::class.java).apply {
            action = "SHOW_OVERLAY"
        })
        
        viewModel.startAnalysis()
        updateStatusText("Analysis started - detecting patterns...")
    }
    
    private fun stopAnalysis() {
        // Stop services
        stopService(Intent(this, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_STOP_CAPTURE
        })
        stopService(Intent(this, PatternDetectionService::class.java))
        startService(Intent(this, OverlayService::class.java).apply {
            action = "HIDE_OVERLAY"
        })
        
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        
        viewModel.stopAnalysis()
        updateStatusText("Analysis stopped")
    }
    
    private fun updatePatternsList(patterns: List<com.jarvis.patternoverlay.model.Pattern>) {
        val patternsText = if (patterns.isEmpty()) {
            "No patterns detected"
        } else {
            patterns.take(5).joinToString("\n") { pattern ->
                "${pattern.name} (${String.format("%.1f%%", pattern.confidence * 100)}) - ${pattern.direction}"
            }
        }
        
        binding.patternsText.text = patternsText
    }
    
    private fun updateSignalDisplay(signal: com.jarvis.patternoverlay.model.PatternSignal?) {
        if (signal != null) {
            val signalText = """
                Signal: ${signal.action}
                Confidence: ${String.format("%.1f%%", signal.confidence * 100)}
                Entry: ${String.format("%.6f", signal.entryPrice)}
                Stop Loss: ${String.format("%.6f", signal.stopLoss)}
                Target 1: ${String.format("%.6f", signal.target1)}
                Target 2: ${String.format("%.6f", signal.target2)}
                Risk:Reward: ${String.format("%.1f", signal.riskRewardRatio)}
                Duration: ${signal.expectedDuration}
            """.trimIndent()
            
            binding.signalText.text = signalText
            binding.signalText.setTextColor(
                when (signal.action) {
                    com.jarvis.patternoverlay.model.SignalAction.BUY -> getColor(R.color.signal_buy)
                    com.jarvis.patternoverlay.model.SignalAction.SELL -> getColor(R.color.signal_sell)
                    else -> getColor(R.color.signal_hold)
                }
            )
        } else {
            binding.signalText.text = "No active signal"
            binding.signalText.setTextColor(getColor(R.color.text_secondary))
        }
    }
    
    private fun updateStatusText(status: String) {
        binding.statusText.text = status
        Timber.d("Status: $status")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
        }
    }
}