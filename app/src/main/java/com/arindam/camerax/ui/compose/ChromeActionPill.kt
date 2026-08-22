package com.arindam.camerax.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arindam.camerax.ui.theme.CameraFontFamily
import com.arindam.camerax.ui.theme.themedOverlayChrome

/**
 * Presentation: full-width glass or accent pill; height matches [ChromeControlSize]
 * (back / motion chip). Use for Retake/Done and permission actions.
 */
@Composable
fun ChromeActionPill(
    icon: ImageVector?,
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chrome = themedOverlayChrome()
    val shape = RoundedCornerShape(ChromeControlSize / 2)
    val contentColor = if (filled) chrome.onAccent else chrome.onGlass
    // Outer Box owns the fixed height so RowScope.weight(fill = true) cannot stretch it.
    Box(
        modifier = modifier
            .height(ChromeControlSize)
            .clip(shape)
            .then(
                if (filled) {
                    Modifier.background(chrome.accent)
                } else {
                    Modifier
                        .background(chrome.glass)
                        .border(1.dp, chrome.stroke, shape)
                }
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                color = contentColor,
                fontFamily = CameraFontFamily,
                fontSize = 14.sp,
                fontWeight = if (filled) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
