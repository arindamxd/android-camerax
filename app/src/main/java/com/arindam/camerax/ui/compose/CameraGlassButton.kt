package com.arindam.camerax.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arindam.camerax.ui.theme.CameraGlassStrong
import com.arindam.camerax.ui.theme.CameraOnGlass

/**
 * Presentation: shared chrome control size (back, Play motion photo, Retake/Done,
 * gallery share/delete, and other glass actions). Keep all of those at this height.
 */
val ChromeControlSize = 44.dp

/**
 * Presentation: circular glass control (live-feed settings, back, and other header actions).
 * Reuse so size, stroke, and icon scale stay identical. Prefer this over one-off diameters.
 */
@Composable
fun CameraGlassButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    diameter: Dp? = null,
    onGlass: Color? = null,
    glass: Color? = null,
    stroke: Color? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val size = diameter ?: if (compact) 40.dp else ChromeControlSize
    val iconSize = when {
        diameter != null -> 22.dp
        compact -> 18.dp
        else -> 20.dp
    }
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(size)
                .clip(CircleShape)
                .background(glass ?: CameraGlassStrong)
                .border(1.dp, stroke ?: Color.White.copy(alpha = 0.14f), CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = onGlass ?: CameraOnGlass,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
