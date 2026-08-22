package com.arindam.camerax.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.arindam.camerax.ui.theme.AppTheme
import com.arindam.camerax.ui.theme.CameraAccent
import com.arindam.camerax.ui.theme.CameraDanger
import com.arindam.camerax.ui.theme.CameraFontFamily

private val AlertTitleSize = 20.sp
private val AlertBodySize = 14.sp
private val AlertActionSize = 14.sp
private val AlertShape = RoundedCornerShape(24.dp)
private val AlertActionShape = RoundedCornerShape(20.dp)

/**
 * Shared confirm / notice sheet used by gallery delete and playback failures so title,
 * body, and action type stay identical.
 */
@Composable
fun CameraAlertDialog(
    show: Boolean,
    title: String,
    text: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit = {},
    dismissLabel: String? = null,
    destructiveConfirm: Boolean = false
) {
    if (!show) return

    val scheme = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AlertShape)
                .background(scheme.surface)
                .border(1.dp, scheme.outline.copy(alpha = 0.4f), AlertShape)
                .padding(20.dp)
        ) {
            Text(
                text = title,
                color = scheme.onSurface,
                fontFamily = CameraFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = AlertTitleSize
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = text,
                color = scheme.onSurfaceVariant,
                fontFamily = CameraFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = AlertBodySize
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dismissLabel != null) {
                    Text(
                        text = dismissLabel,
                        color = scheme.onSurface,
                        fontFamily = CameraFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = AlertActionSize,
                        modifier = Modifier
                            .clip(AlertActionShape)
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
                Text(
                    text = confirmLabel,
                    color = Color.Black,
                    fontFamily = CameraFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = AlertActionSize,
                    modifier = Modifier
                        .clip(AlertActionShape)
                        .background(if (destructiveConfirm) CameraDanger else CameraAccent)
                        .clickable {
                            onDismiss()
                            onConfirm()
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@DarkLightPreviews
@Composable
private fun CameraAlertDialogPreview() {
    AppTheme {
        CameraAlertDialog(
            show = true,
            title = "Unavailable",
            text = "This video could not be played.",
            confirmLabel = "OK",
            onDismiss = {}
        )
    }
}
