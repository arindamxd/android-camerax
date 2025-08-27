package com.arindam.camerax.ui.home

import android.os.Bundle
import com.arindam.camerax.databinding.ActivityHomeBinding
import com.arindam.camerax.ui.base.BaseActivity

/**
 * Main entry point into our app. This app follows the single-activity pattern, and all
 * functionality is implemented in the form of fragments.
 * Created by Arindam Karmakar on 17/04/20.
 */

class HomeActivity : BaseActivity<ActivityHomeBinding>() {

    override fun provideBinding(): ActivityHomeBinding = ActivityHomeBinding.inflate(layoutInflater)

    override fun setupView(savedInstanceState: Bundle?) {
        // Empty
    }
}
