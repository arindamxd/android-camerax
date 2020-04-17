package com.arindam.camerax.ui.base

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.arindam.camerax.utils.FLAGS_FULLSCREEN

/**
 * Created by Arindam Karmakar on 17/04/20.
 */

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.systemUiVisibility = FLAGS_FULLSCREEN;
        super.onCreate(savedInstanceState)
        setContentView(provideLayout())
        setupView(savedInstanceState)
    }

    @LayoutRes
    abstract fun provideLayout(): Int
    abstract fun setupView(savedInstanceState: Bundle?)
}
