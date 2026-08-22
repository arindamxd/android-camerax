package com.arindam.camerax.ui.home.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.arindam.camerax.di.AppContainer

/**
 * Builds [CameraViewModel] with [com.arindam.camerax.di.CameraInteractors] from [AppContainer].
 */
class CameraViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (!modelClass.isAssignableFrom(CameraViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
        return CameraViewModel(
            interactors = container.cameraInteractors,
            savedState = extras.createSavedStateHandle(),
            dispatchers = container.dispatchers
        ) as T
    }
}
