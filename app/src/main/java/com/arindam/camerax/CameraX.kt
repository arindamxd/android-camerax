package com.arindam.camerax

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.arindam.camerax.data.local.Preferences
import com.arindam.camerax.util.commons.Constants
import com.arindam.camerax.util.theme.NightMode
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

/**
 * Created by Arindam Karmakar on 17/04/20.
 */

class CameraX : Application() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate() {
        super.onCreate()

        Preferences.init(this)

        handleDayNightTheme()
        handleFirebaseData()
    }

    /* Handle Theme */
    private fun handleDayNightTheme() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        preferences.getString(getString(R.string.pref_key_theme), getString(R.string.pref_key_theme_default))?.apply {
            val mode = NightMode.valueOf(this.toUpperCase(Locale.US))
            AppCompatDelegate.setDefaultNightMode(mode.value)
        }
    }

    /* Handle Firestore */
    private fun handleFirebaseData() {
        db = Firebase.firestore
        db.document(Constants.FIRESTORE.DOCUMENT_PATH).addSnapshotListener { document, exception ->
            exception?.let { return@addSnapshotListener }
            Preferences.setQuoteMessage(document?.getString(Constants.FIRESTORE.KEY_MESSAGE))
            Preferences.setQuoteAuthor(document?.getString(Constants.FIRESTORE.KEY_AUTHOR))
        }
    }
}
