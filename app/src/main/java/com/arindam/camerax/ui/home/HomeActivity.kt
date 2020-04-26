package com.arindam.camerax.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.arindam.camerax.R
import com.arindam.camerax.ui.base.BaseActivity
import com.arindam.camerax.util.commons.Constants

/**
 * Main entry point into our app. This app follows the single-activity pattern, and all
 * functionality is implemented in the form of fragments.
 * Created by Arindam Karmakar on 17/04/20.
 */

class HomeActivity : BaseActivity() {

    override fun provideLayout(): Int = R.layout.activity_home

    override fun setupView(savedInstanceState: Bundle?) {

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
