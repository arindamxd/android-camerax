package com.arindam.camerax.ui.home.gallery

import androidx.lifecycle.ViewModel
import com.arindam.camerax.di.CameraInteractors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/** Presentation state for the in-app gallery pager. */
data class GalleryUiState(
    val items: List<File> = emptyList(),
    val videoAutoplay: Boolean = false
)

/**
 * Gallery list and delete. Share stays in the fragment (FileProvider). Calls
 * [CameraInteractors] only.
 */
class GalleryViewModel(
    private val interactors: CameraInteractors,
    private val directory: File
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                items = interactors.listMedia(directory),
                videoAutoplay = interactors.loadCaptureSettings().galleryVideoAutoplay
            )
        }
    }

    fun delete(file: File) {
        interactors.deleteMedia(file)
        refresh()
    }
}
