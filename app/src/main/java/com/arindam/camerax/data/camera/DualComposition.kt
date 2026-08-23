package com.arindam.camerax.data.camera

/**
 * Data: Dual concurrent composition layout ([androidx.camera.core.CompositionSettings] NDC).
 *
 * NDC: origin at frame center, +X right, +Y up, range [-1, 1]. Offset is applied after scale.
 * Composition uses the PreviewView [androidx.camera.core.ViewPort], so NDC maps to the full view.
 */
internal object DualComposition {
    const val PIP_OFFSET_X = 0.58f
    const val PIP_OFFSET_Y = -0.18f
    const val PIP_SCALE = 0.28f
}
