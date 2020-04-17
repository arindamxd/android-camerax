package com.arindam.camerax.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.arindam.camerax.R

/**
 * Created by Arindam Karmakar on 17/04/20.
 */

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
