package com.arindam.camerax.util.display

import android.content.Context
import android.graphics.PorterDuff
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.arindam.camerax.R
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
        val toast = Toast.makeText(context, text ?: "", Toast.LENGTH_SHORT)
        toast.view.background.setColorFilter(ContextCompat.getColor(context, R.color.black), PorterDuff.Mode.SRC_IN)
        toast.setGravity(Gravity.CENTER, 0, 0)
        val textView = toast.view.findViewById<TextView>(android.R.id.message)
        textView.setTextColor(ContextCompat.getColor(context, R.color.white))
        toast.show()
    }

    fun showSnack(view: View, text: CharSequence?) {
        Snackbar.make(view, text ?: "", Snackbar.LENGTH_LONG).show()
    }
}
