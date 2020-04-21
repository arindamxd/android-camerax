package com.arindam.camerax.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.ListPopupWindow
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.arindam.camerax.R
import com.arindam.camerax.utils.display.Toaster
import com.arindam.camerax.utils.theme.NightMode
import java.util.*

/**
 * Created by Arindam Karmakar on 17/04/20.
 */

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) = setPreferencesFromResource(R.xml.preferences, rootKey)

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return when (preference?.key) {
            resources.getString(R.string.pref_key_theme) -> {
                themePopupMenu(listView.getChildAt(preference.order)).also { it.show() }
                true
            }
            else -> false
        }
    }

    private fun themePopupMenu(view: View): ListPopupWindow {
        val keys = resources.getStringArray(R.array.pref_key_theme_popup)
        val values = resources.getStringArray(R.array.pref_key_theme_popup_values)
        val adapter: ArrayAdapter<CharSequence> = ArrayAdapter<CharSequence>(requireContext(), R.layout.preference_theme_item, keys)

        return ListPopupWindow(requireContext(), null, R.attr.listPopupWindowStyle).apply {
            setAdapter(adapter)
            anchorView = view
            setOnItemClickListener { parent, view, position, id ->
                updateTheme(values[position])
                dismiss()
            }
        }
    }

    private fun updateTheme(theme: String) {
        preferenceManager.sharedPreferences.edit().putString(getString(R.string.pref_key_theme), theme).apply()
        val mode = NightMode.valueOf(theme.toUpperCase(Locale.US))
        AppCompatDelegate.setDefaultNightMode(mode.value)
        requireActivity().recreate()
    }
}
