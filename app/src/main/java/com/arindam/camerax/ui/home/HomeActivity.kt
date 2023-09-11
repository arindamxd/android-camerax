package com.arindam.camerax.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.arindam.camerax.databinding.ActivityHomeBinding
import com.arindam.camerax.ui.base.BaseActivity
import com.arindam.camerax.util.commons.Constants

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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val intent = Intent(Constants.EXTRAS.KEY_EVENT_ACTION).apply {
                    putExtra(Constants.EXTRAS.KEY_EVENT_EXTRA, keyCode)
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
