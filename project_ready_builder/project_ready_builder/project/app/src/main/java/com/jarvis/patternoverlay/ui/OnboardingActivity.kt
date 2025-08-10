package com.jarvis.patternoverlay.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.jarvis.patternoverlay.R
import com.jarvis.patternoverlay.databinding.ActivityOnboardingBinding
import com.jarvis.patternoverlay.ui.adapter.OnboardingPagerAdapter

class OnboardingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var pagerAdapter: OnboardingPagerAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViewPager()
        setupButtons()
    }
    
    private fun setupViewPager() {
        pagerAdapter = OnboardingPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.dotsIndicator.attachTo(binding.viewPager)
        
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtons(position)
            }
        })
    }
    
    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < pagerAdapter.itemCount - 1) {
                binding.viewPager.currentItem = current + 1
            } else {
                completeOnboarding()
            }
        }
        
        binding.btnSkip.setOnClickListener {
            completeOnboarding()
        }
        
        binding.btnPrevious.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current > 0) {
                binding.viewPager.currentItem = current - 1
            }
        }
    }
    
    private fun updateButtons(position: Int) {
        binding.btnPrevious.isEnabled = position > 0
        
        if (position == pagerAdapter.itemCount - 1) {
            binding.btnNext.text = getString(R.string.done)
            binding.btnSkip.visibility = Button.GONE
        } else {
            binding.btnNext.text = getString(R.string.next)
            binding.btnSkip.visibility = Button.VISIBLE
        }
    }
    
    private fun completeOnboarding() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}