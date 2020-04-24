package com.arindam.camerax.data.local

import android.content.Context
import android.content.SharedPreferences
import com.arindam.camerax.utils.commons.Constants

/**
 * Created by Arindam Karmakar on 24/04/20.
 */

object Preferences {

    private const val KEY_QUOTE_MESSAGE = "key_quote_message"
    private const val KEY_QUOTE_AUTHOR = "key_quote_author"

    private var pref: SharedPreferences? = null
    fun init(context: Context) = context.getSharedPreferences(Constants.INTERNAL.PREF_NAME, Context.MODE_PRIVATE).also { pref = it }

    internal fun getQuoteMessage(): String = pref?.getString(KEY_QUOTE_MESSAGE, "#StayHome") ?: "#StayHome"
    internal fun setQuoteMessage(message: String?) = pref?.edit()?.apply { putString(KEY_QUOTE_MESSAGE, message).apply() }

    internal fun getQuoteAuthor(): String = pref?.getString(KEY_QUOTE_AUTHOR, "CameraX") ?: "CameraX"
    internal fun setQuoteAuthor(author: String?) = pref?.edit()?.apply { putString(KEY_QUOTE_AUTHOR, author).apply() }
}
