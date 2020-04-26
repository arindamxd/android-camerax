package com.arindam.camerax.ui.settings

import android.os.Bundle
import com.arindam.camerax.R
import com.arindam.camerax.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_settings.*

/**
 * Created by Arindam Karmakar on 18/04/20.
 */

class SettingsActivity : BaseActivity() {

    override fun provideLayout(): Int = R.layout.activity_settings

    override fun setupView(savedInstanceState: Bundle?) {
        setupToolBar()
        setupFragment()
    }

    private fun setupToolBar() {
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
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
