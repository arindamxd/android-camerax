package com.arindam.camerax.ui.home.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arindam.camerax.data.camera.MotionPhotoMuxer
import com.arindam.camerax.data.media.isRawCapture
import com.arindam.camerax.data.media.isVideoCapture
import com.arindam.camerax.data.media.mediaFileInfo
import com.arindam.camerax.di.AppDispatchers
import com.arindam.camerax.di.CameraInteractors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Presentation: one pager page in the in-app gallery. */
data class GalleryItem(
    val file: File,
    val isVideo: Boolean,
    val isMotionPhoto: Boolean,
    val isRaw: Boolean,
    val width: Int = 0,
    val height: Int = 0,
    val durationNanos: Long? = null
)

/** Presentation state for the in-app gallery pager. */
data class GalleryUiState(
    val items: List<GalleryItem> = emptyList(),
    val videoAutoplay: Boolean = false,
    val loading: Boolean = true
)

/**
 * Presentation: gallery list and delete. Share stays in the fragment (FileProvider). Calls
 * [CameraInteractors] only. Disk work runs on [AppDispatchers.io].
 */
class GalleryViewModel(
    private val interactors: CameraInteractors,
    private val directory: File,
    private val dispatchers: AppDispatchers = AppDispatchers()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val files = interactors.listMedia(directory)
            val items = withContext(dispatchers.io) {
                files.map { file ->
                    val info = mediaFileInfo(file)
                    GalleryItem(
                        file = file,
                        isVideo = file.isVideoCapture(),
                        isMotionPhoto = MotionPhotoMuxer.isMotionPhoto(file),
                        isRaw = file.isRawCapture(),
                        width = info.width,
                        height = info.height,
                        durationNanos = info.durationNanos
                    )
                }
            }
            val autoplay = interactors.loadCaptureSettings().galleryVideoAutoplay
            _uiState.update {
                it.copy(items = items, videoAutoplay = autoplay, loading = false)
            }
        }
    }

    fun delete(file: File) {
        viewModelScope.launch {
            interactors.deleteMedia(file)
            refresh()
        }
    }
}
