package com.arindam.camerax.util.display

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

/**
 * Created by Arindam Karmakar on 17/04/20.
 */

object Toaster {

    /**
     * Show Custom [Toast]
     *
     * @param context [Context]
     * @param text    Message to Print
     */
    fun show(context: Context, text: CharSequence?) {
        Toast.makeText(context.applicationContext, text ?: "", Toast.LENGTH_SHORT).show()
    }

    fun showSnack(view: View, text: CharSequence?) {
        Snackbar.make(view, text ?: "", Snackbar.LENGTH_LONG).show()
    }
}
