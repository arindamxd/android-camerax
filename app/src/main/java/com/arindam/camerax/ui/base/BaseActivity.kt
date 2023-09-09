package com.arindam.camerax.ui.base

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.arindam.camerax.ui.home.HomeActivity

/**
 * Created by Arindam Karmakar on 17/04/20.
 */

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (this is HomeActivity) installSplashScreen()
        super.onCreate(savedInstanceState)
        provideLayout()?.let { setContentView(it) }
        setupView(savedInstanceState)
    }

    open fun provideLayout(): View? = null
    abstract fun setupView(savedInstanceState: Bundle?)
}
