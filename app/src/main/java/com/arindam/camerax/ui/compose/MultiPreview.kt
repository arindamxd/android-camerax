package com.arindam.camerax.ui.compose

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Created by Arindam Karmakar on 13/09/23.
 */

@Preview(
    name = "Small font",
    group = "Font scales",
    fontScale = 0.5f
)
@Preview(
    name = "Large font",
    group = "Font scales",
    fontScale = 1.5f
)
annotation class FontScalePreviews

@Preview(
    name = "Light mode",
    group = "UI mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL,
    showBackground = true
)
@Preview(
    name = "Dark mode",
    group = "UI mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    showBackground = true
)
annotation class LightDarkPreviews
