package com.arindam.camerax.ui.home.camera

import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import com.arindam.camerax.R
import com.arindam.camerax.data.camera.MotionPhotoMuxer
import com.arindam.camerax.ui.compose.ChromeActionPill
import com.arindam.camerax.ui.theme.CameraAccent
import com.arindam.camerax.ui.theme.CameraMono
import com.arindam.camerax.ui.theme.themedOverlayChrome
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Optional Retake / Done overlay after a still or clip. Off by default in Settings. */
@Composable
fun CaptureConfirmOverlay(
    review: CaptureReview,
    onRetake: () -> Unit,
    onKeep: () -> Unit
) {
    BackHandler(onBack = onKeep)
    val chrome = themedOverlayChrome()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(2f)
            .background(chrome.canvas)
    ) {
        when {
            review.isVideo -> ReviewVideo(file = review.file)
            review.isMotionPhoto -> ReviewMotionStill(file = review.file)
            else -> Image(
                painter = rememberAsyncImagePainter(model = review.file),
                contentDescription = stringResource(R.string.capture_review_photo),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(chrome.scrim.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, chrome.scrim.copy(alpha = 0.62f))
                    )
                )
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing
                        .union(WindowInsets.systemGestures)
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(chrome.glass)
                    .border(1.dp, chrome.stroke, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onKeep
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_button_alt),
                    tint = chrome.onGlass,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (review.isMotionPhoto) {
                Text(
                    text = stringResource(R.string.motion_photo_badge),
                    color = CameraAccent,
                    fontFamily = CameraMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(chrome.glass)
                        .border(1.dp, chrome.stroke, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing
                        .union(WindowInsets.systemGestures)
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = review.metadataLabel(
                    formatText = review.formatLabelRes?.let { stringResource(it) }
                ),
                color = chrome.muted,
                fontFamily = CameraMono,
                fontSize = 10.sp,
                letterSpacing = 0.06.em,
                modifier = Modifier.padding(
                    bottom = if (review.isVideo) 12.dp else 8.dp
                )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChromeActionPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Refresh,
                    label = stringResource(R.string.capture_review_retake),
                    filled = false,
                    onClick = onRetake
                )
                ChromeActionPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Check,
                    label = stringResource(R.string.capture_review_keep),
                    filled = true,
                    onClick = onKeep
                )
            }
        }
    }
}

@Composable
private fun ReviewMotionStill(file: File) {
    val context = LocalContext.current
    var playing by remember(file) { mutableStateOf(false) }
    val clip = remember(file) {
        File(context.cacheDir, "review_motion_${file.nameWithoutExtension}.mp4")
    }
    var extracted by remember(file) { mutableStateOf<File?>(null) }
    LaunchedEffect(file, playing) {
        if (!playing) {
            extracted = null
            return@LaunchedEffect
        }
        val result = withContext(Dispatchers.IO) {
            runCatching { MotionPhotoMuxer.extractVideo(file, clip) }.getOrNull()
        }
        extracted = result
        if (result == null) playing = false
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { playing = !playing }
    ) {
        if (playing && extracted != null) {
            ReviewVideo(file = extracted!!)
        } else {
            Image(
                painter = rememberAsyncImagePainter(model = file),
                contentDescription = stringResource(R.string.capture_review_photo),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ReviewVideo(file: File) {
    val videoView = remember(file) { mutableStateOf<VideoView?>(null) }
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setVideoPath(file.absolutePath)
                setOnPreparedListener { player ->
                    player.isLooping = true
                    start()
                }
                setOnClickListener {
                    if (isPlaying) pause() else start()
                }
                videoView.value = this
            }
        },
        modifier = Modifier.fillMaxSize()
    )
    DisposableEffect(file) {
        onDispose { videoView.value?.stopPlayback() }
    }
}
