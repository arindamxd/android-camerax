package com.arindam.camerax.ui.home.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arindam.camerax.di.CameraInteractors

class CameraViewModelFactory(
    private val interactors: CameraInteractors
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
            return CameraViewModel(interactors) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
