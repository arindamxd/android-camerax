package com.arindam.camerax.ui.settings

import android.os.Bundle
import android.view.View
import com.arindam.camerax.R
import com.arindam.camerax.databinding.ActivitySettingsBinding
import com.arindam.camerax.ui.base.BaseActivity

/**
 * Created by Arindam Karmakar on 18/04/20.
 */

class SettingsActivity : BaseActivity() {

    val binding by lazy { ActivitySettingsBinding.inflate(layoutInflater) }

    override fun provideLayout(): View = binding.root

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
