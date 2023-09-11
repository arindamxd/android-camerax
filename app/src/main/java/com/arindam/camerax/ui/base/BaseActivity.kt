package com.arindam.camerax.ui.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.viewbinding.ViewBinding
import com.arindam.camerax.ui.home.HomeActivity

/**
 * Created by Arindam Karmakar on 17/04/20.
 */

abstract class BaseActivity<T : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: T

    override fun onCreate(savedInstanceState: Bundle?) {
        if (this is HomeActivity) installSplashScreen()
        super.onCreate(savedInstanceState)
        provideBinding().let {
            if (it == null) setContentView(0)
            else {
                binding = it
                setContentView(binding.root)
            }
        }
        setupView(savedInstanceState)
    }

    open fun provideBinding(): T? = null
    abstract fun setupView(savedInstanceState: Bundle?)
}
