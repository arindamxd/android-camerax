package com.arindam.camerax.ui.home.camera

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arindam.camerax.di.CameraInteractors

/**
 * Builds [CameraViewModel] with [CameraInteractors] from [com.arindam.camerax.di.AppContainer].
 */
class CameraViewModelFactory(
    private val interactors: CameraInteractors,
    context: Context
) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
            return CameraViewModel(interactors, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
