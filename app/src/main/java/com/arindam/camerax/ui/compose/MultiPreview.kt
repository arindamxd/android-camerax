package com.arindam.camerax.ui.compose

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Created by Arindam Karmakar on 13/09/23.
 */

@Preview(
    name = "Small Font",
    group = "Font Scales",
    fontScale = 0.5f
)
@Preview(
    name = "Large Font",
    group = "Font Scales",
    fontScale = 1.5f
)
annotation class FontScalePreviews

@Preview(
    name = "Dark Mode",
    group = "UI Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    showBackground = true,
    showSystemUi = true
)
@Preview(
    name = "Light Mode",
    group = "UI Mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL,
    showBackground = true,
    showSystemUi = true
)
annotation class DarkLightPreviews
