package com.arindam.editorx

import com.arindam.camerax.interfaces.DynamicInterface

/**
 * Created by Arindam Karmakar on 27/9/19.
 */

object FetchValue : DynamicInterface {
    override fun getVersionName(): String = BuildConfig.VERSION_NAME
}
