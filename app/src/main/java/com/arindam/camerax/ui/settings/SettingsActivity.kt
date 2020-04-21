package com.arindam.camerax.ui.settings

import android.os.Bundle
import com.arindam.camerax.ui.base.BaseActivity

/**
 * Created by Arindam Karmakar on 18/04/20.
 */

class SettingsActivity : BaseActivity() {

    override fun setupView(savedInstanceState: Bundle?) {
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }
}
