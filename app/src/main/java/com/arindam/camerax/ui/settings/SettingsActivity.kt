package com.arindam.camerax.ui.settings

import android.os.Bundle
import com.arindam.camerax.R
import com.arindam.camerax.databinding.ActivitySettingsBinding
import com.arindam.camerax.ui.base.BaseActivity

/**
 * Created by Arindam Karmakar on 18/04/20.
 */

class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {

    override fun provideBinding(): ActivitySettingsBinding = ActivitySettingsBinding.inflate(layoutInflater)

    override fun setupView(savedInstanceState: Bundle?) {
        setupToolBar()
        setupFragment()
    }

    private fun setupToolBar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupFragment() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, SettingsFragment())
            .commit()
    }
}
