package com.arindam.camerax.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.arindam.camerax.ui.theme.CameraAccent
import com.arindam.camerax.ui.theme.CameraFontFamily
import com.arindam.camerax.ui.theme.themedOverlayChrome

/** Full-width glass or accent pill used on capture confirm and permission screens. */
@Composable
fun ChromeActionPill(
    icon: ImageVector?,
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chrome = themedOverlayChrome()
    val shape = RoundedCornerShape(26.dp)
    val contentColor = if (filled) Color.Black else chrome.onGlass
    val base = modifier
        .height(52.dp)
        .clip(shape)
    val styled = if (filled) {
        base.background(CameraAccent)
    } else {
        base
            .background(chrome.glass)
            .border(1.dp, chrome.stroke, shape)
    }
    Row(
        modifier = styled.clickable(onClick = onClick),
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
