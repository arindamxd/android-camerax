package com.arindam.camerax.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arindam.camerax.di.AppDispatchers
import com.arindam.camerax.di.CameraInteractors
import com.arindam.camerax.domain.model.DeviceCaptureFeatures
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val features: DeviceCaptureFeatures = DeviceCaptureFeatures(),
    val versionLabel: String = ""
)

/**
 * Settings capabilities. Preference rows still persist via SharedPreferences in the screen;
 * camera bind flags are read through [LoadCaptureSettings][com.arindam.camerax.domain.usecase.LoadCaptureSettings].
 */
class SettingsViewModel(
    private val interactors: CameraInteractors,
    versionLabel: String,
    private val dispatchers: AppDispatchers = AppDispatchers()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(versionLabel = versionLabel))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val features = withContext(dispatchers.default) {
                interactors.probeDeviceFeatures()
            }
            _uiState.update { it.copy(features = features) }
        }
    }
}
