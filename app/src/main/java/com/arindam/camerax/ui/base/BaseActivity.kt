package com.arindam.camerax.ui.base

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity

/**
 * Created by Arindam Karmakar on 17/04/20.
 */

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        provideLayout()?.let { setContentView(it) }
        setupView(savedInstanceState)
    }

    @LayoutRes
    open fun provideLayout(): Int? = null
    abstract fun setupView(savedInstanceState: Bundle?)
}
