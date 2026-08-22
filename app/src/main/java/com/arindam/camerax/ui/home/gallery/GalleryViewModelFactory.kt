package com.arindam.camerax.ui.home.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arindam.camerax.di.AppDispatchers
import com.arindam.camerax.di.CameraInteractors
import java.io.File

/**
 * Presentation: builds [GalleryViewModel] with [CameraInteractors] from [com.arindam.camerax.di.AppContainer].
 */
class GalleryViewModelFactory(
    private val interactors: CameraInteractors,
    private val directory: File,
    private val dispatchers: AppDispatchers = AppDispatchers()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            return GalleryViewModel(interactors, directory, dispatchers) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
