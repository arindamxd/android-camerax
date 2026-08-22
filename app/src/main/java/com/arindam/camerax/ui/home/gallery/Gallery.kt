@file:OptIn(ExperimentalFoundationApi::class)

package com.arindam.camerax.ui.home.gallery

import android.widget.VideoView
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.arindam.camerax.R
import com.arindam.camerax.data.camera.MotionPhotoMuxer
import com.arindam.camerax.data.media.mediaFileInfo
import com.arindam.camerax.ui.compose.DarkLightPreviews
import com.arindam.camerax.ui.home.camera.formatRecordingTime
import com.arindam.camerax.ui.theme.AppTheme
import com.arindam.camerax.ui.theme.CameraAccent
import com.arindam.camerax.ui.theme.CameraDanger
import com.arindam.camerax.ui.theme.CameraFontFamily
import com.arindam.camerax.ui.theme.CameraMono
import com.arindam.camerax.ui.theme.themedOverlayChrome
import java.io.File

/** Full-screen pager over captured files (photos, video, motion, DNG). */
@Composable
fun GalleryScreen(
    items: List<File> = emptyList(),
    navigateBack: () -> Unit,
    onShareClicked: (Int) -> Unit,
    onDelete: (File) -> Unit
) {
    val chrome = themedOverlayChrome()
    val pagerState = rememberPagerState(pageCount = { items.size })
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var motionPlaying by remember { mutableStateOf(false) }
    val current = items.getOrNull(pagerState.currentPage)
    val isMotion = current != null && MotionPhotoMuxer.isMotionPhoto(current)
    val isVideo = current?.extension?.equals("mp4", ignoreCase = true) == true
    val formatText = if (current?.extension?.equals("dng", ignoreCase = true) == true) {
        stringResource(R.string.raw_dng)
    } else {
        null
    }
    val metadataLabel = remember(current, formatText) {
        current?.let { galleryMetadataLabel(it, formatText) }.orEmpty()
    }

    LaunchedEffect(pagerState.currentPage) {
        playbackSpeed = 1f
        motionPlaying = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(chrome.canvas)
    ) {
        GalleryPager(
            items = items,
            pagerState = pagerState,
            playbackSpeed = playbackSpeed,
            motionPlaying = motionPlaying
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
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
        GalleryHeader(
            showMotion = isMotion,
            motionPlaying = motionPlaying,
            onMotionClicked = { motionPlaying = !motionPlaying },
            navigateBack = navigateBack
        )
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
            if (metadataLabel.isNotEmpty()) {
                Text(
                    text = metadataLabel,
                    color = chrome.muted,
                    fontFamily = CameraMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.06.em,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 20.dp, bottom = if (isVideo) 12.dp else 8.dp)
                )
            }
            if (isVideo) {
                PlaybackSpeedRow(
                    speed = playbackSpeed,
                    onSpeedSelected = { playbackSpeed = it }
                )
            }
            GalleryFooter(
                items = items,
                pagerState = pagerState,
                onShareClicked = onShareClicked,
                onDelete = onDelete
            )
        }
    }
}

@DarkLightPreviews
@Composable
private fun GalleryScreenPreview() {
    AppTheme {
        GalleryScreen(
            navigateBack = {},
            onShareClicked = {},
            onDelete = {}
        )
    }
}

@Composable
private fun GalleryHeader(
    showMotion: Boolean,
    motionPlaying: Boolean,
    onMotionClicked: () -> Unit,
    navigateBack: () -> Unit
) {
    val chrome = themedOverlayChrome()
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing
                    .union(WindowInsets.systemGestures)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
            .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
    ) {
        GalleryActionButton(
            icon = R.drawable.ic_back,
            contentDescription = stringResource(R.string.back_button_alt),
            onClick = navigateBack
        )
        if (showMotion) {
            Text(
                text = stringResource(
                    if (motionPlaying) R.string.motion_photo_badge else R.string.play_motion_photo
                ),
                color = if (motionPlaying) Color.Black else CameraAccent,
                fontFamily = CameraMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (motionPlaying) CameraAccent else chrome.glass)
                    .border(1.dp, chrome.stroke, RoundedCornerShape(20.dp))
                    .clickable(onClick = onMotionClicked)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        } else {
            Spacer(Modifier.size(48.dp))
        }
    }
}

@Composable
private fun GalleryFooter(
    items: List<File>,
    pagerState: PagerState,
    onShareClicked: (Int) -> Unit,
    onDelete: (File) -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }
    DeleteDialog(
        show = showDialog.value,
        onConfirmed = {
            items.getOrNull(pagerState.currentPage)?.let(onDelete)
        },
        onDismiss = { showDialog.value = false }
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        if (items.isEmpty()) return@Row
        GalleryActionButton(
            icon = R.drawable.ic_share,
            contentDescription = stringResource(R.string.share_button_alt),
            onClick = { onShareClicked.invoke(pagerState.currentPage) }
        )
        GalleryActionButton(
            icon = R.drawable.ic_delete,
            contentDescription = stringResource(R.string.delete_button_alt),
            onClick = { showDialog.value = true }
        )
    }
}

@Composable
private fun GalleryActionButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    val chrome = themedOverlayChrome()
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(44.dp)
            .clip(CircleShape)
            .background(chrome.glass)
            .border(1.dp, chrome.stroke, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = remember { ripple(bounded = true) },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = icon),
            modifier = Modifier.size(20.dp),
            tint = chrome.onGlass,
            contentDescription = contentDescription
        )
    }
}

@Composable
private fun GalleryPager(
    items: List<File>,
    pagerState: PagerState,
    playbackSpeed: Float = 1f,
    motionPlaying: Boolean = false,
) {
    HorizontalPager(
        state = pagerState,
        pageSize = PageSize.Fill,
        beyondViewportPageCount = 1,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        items.getOrNull(page)?.let { file ->
            if (file.extension.lowercase() == "mp4") {
                GalleryVideo(
                    file = file,
                    isActive = pagerState.currentPage == page,
                    playbackSpeed = playbackSpeed
                )
            } else {
                val motion = MotionPhotoMuxer.isMotionPhoto(file)
                Box(Modifier.fillMaxSize()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = file),
                        contentScale = ContentScale.Fit,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (motion) {
                        GalleryMotionOverlay(file = file, playing = motionPlaying)
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryMotionOverlay(file: File, playing: Boolean) {
    val context = LocalContext.current
    val clip = remember(file) {
        File(context.cacheDir, "motion_${file.nameWithoutExtension}.mp4")
    }
    if (!playing) return
    val extracted = remember(file) {
        runCatching { MotionPhotoMuxer.extractVideo(file, clip) }.getOrNull()
    }
    if (extracted != null) {
        GalleryVideo(file = extracted, isActive = true, playbackSpeed = 1f)
    }
}

@Composable
private fun PlaybackSpeedRow(
    speed: Float,
    onSpeedSelected: (Float) -> Unit
) {
    val speeds = listOf(0.5f, 1f, 1.5f, 2f)
    val chrome = themedOverlayChrome()
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(chrome.glass)
            .border(1.dp, chrome.stroke, RoundedCornerShape(28.dp))
            .padding(4.dp)
    ) {
        speeds.forEach { value ->
            val selected = kotlin.math.abs(speed - value) < 0.01f
            val label = when {
                kotlin.math.abs(value - 0.5f) < 0.01f -> ".5×"
                value % 1f == 0f -> "${value.toInt()}×"
                else -> "${value}×"
            }
            key(value) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) CameraAccent else chrome.chipIdle
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = { onSpeedSelected(value) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.Black else chrome.onGlass,
                        fontFamily = CameraMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (value < 1f) 11.sp else 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryVideo(file: File, isActive: Boolean, playbackSpeed: Float) {
    val videoView = remember(file) { mutableStateOf<VideoView?>(null) }
    val playerRef = remember(file) { mutableStateOf<android.media.MediaPlayer?>(null) }
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setVideoPath(file.absolutePath)
                setOnPreparedListener { player ->
                    playerRef.value = player
                    player.isLooping = true
                    runCatching {
                        player.playbackParams = player.playbackParams.setSpeed(playbackSpeed)
                    }
                    if (isActive) start()
                }
                videoView.value = this
            }
        },
        update = { view ->
            playerRef.value?.let { player ->
                runCatching {
                    player.playbackParams = player.playbackParams.setSpeed(playbackSpeed)
                }
            }
            if (isActive) {
                if (!view.isPlaying) view.start()
            } else if (view.isPlaying) {
                view.pause()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
    DisposableEffect(file) {
        onDispose {
            videoView.value?.stopPlayback()
            playerRef.value = null
        }
    }
}

@Composable
fun DeleteDialog(
    show: Boolean = false,
    title: Int = R.string.delete_title,
    text: Int = R.string.delete_subtitle,
    confirmTitle: Int = R.string.delete_button_alt,
    dismissTitle: Int = R.string.delete_button_cancel,
    onConfirmed: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

    val shape = RoundedCornerShape(24.dp)
    val scheme = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(scheme.surface)
                .border(1.dp, scheme.outline.copy(alpha = 0.4f), shape)
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(id = title),
                color = scheme.onSurface,
                fontFamily = CameraFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(id = text),
                color = scheme.onSurfaceVariant,
                fontFamily = CameraFontFamily,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = dismissTitle),
                    color = scheme.onSurface,
                    fontFamily = CameraFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
                Text(
                    text = stringResource(id = confirmTitle),
                    color = Color.Black,
                    fontFamily = CameraFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(CameraDanger)
                        .clickable {
                            onDismiss.invoke()
                            onConfirmed.invoke()
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

private fun galleryMetadataLabel(file: File, formatText: String?): String {
    val info = mediaFileInfo(file)
    val size = if (info.width > 0 && info.height > 0) "${info.width} × ${info.height}" else null
    val duration = info.durationNanos?.let { formatRecordingTime(it) }
    return listOfNotNull(duration, size, formatText).joinToString(" · ")
}
