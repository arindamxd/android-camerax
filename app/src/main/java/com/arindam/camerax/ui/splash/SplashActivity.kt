package com.arindam.camerax.ui.splash

import android.content.Intent
import android.os.Bundle
import com.arindam.camerax.R
import com.arindam.camerax.data.local.Preferences
import com.arindam.camerax.ui.base.BaseActivity
import com.arindam.camerax.ui.home.HomeActivity
import kotlinx.android.synthetic.main.activity_splash.*

/**
 * Created by Arindam Karmakar on 17/04/20.
 */

class SplashActivity : BaseActivity() {

    private val completed = 1.0F

    override fun provideLayout(): Int = R.layout.activity_splash

    override fun setupView(savedInstanceState: Bundle?) {

        quote_message.text = Preferences.getQuoteMessage()
        quote_author.text = Preferences.getQuoteAuthor()

        splash_animation.addAnimatorUpdateListener { animation ->
            if (animation.animatedFraction == completed) {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        }
    }
}