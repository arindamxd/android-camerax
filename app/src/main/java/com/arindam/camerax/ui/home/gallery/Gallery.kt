@file:OptIn(ExperimentalFoundationApi::class)

package com.arindam.camerax.ui.home.gallery

import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import coil.compose.rememberAsyncImagePainter
import com.arindam.camerax.R
import com.arindam.camerax.data.camera.MotionPhotoMuxer
import com.arindam.camerax.data.media.mediaFileInfo
import com.arindam.camerax.ui.compose.CameraAlertDialog
import com.arindam.camerax.ui.compose.CameraGlassButton
import com.arindam.camerax.ui.compose.DarkLightPreviews
import com.arindam.camerax.ui.home.camera.formatRecordingTime
import com.arindam.camerax.ui.theme.AppTheme
import com.arindam.camerax.ui.theme.CameraAccent
import com.arindam.camerax.ui.theme.CameraMono
import com.arindam.camerax.ui.theme.themedOverlayChrome
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Full-screen pager over captured files (photos, video, motion, DNG). */
@Composable
fun GalleryScreen(
    items: List<File> = emptyList(),
    videoAutoplay: Boolean = false,
    navigateBack: () -> Unit,
    onShareClicked: (Int) -> Unit,
    onDelete: (File) -> Unit
) {
    val chrome = themedOverlayChrome()
    val pagerState = rememberPagerState(pageCount = { items.size })
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var motionPlaying by remember { mutableStateOf(false) }
    var videoPlaying by remember { mutableStateOf(false) }
    var videoControlsVisible by remember { mutableStateOf(true) }
    var videoControlsTick by remember { mutableIntStateOf(0) }
    var showPlayError by remember { mutableStateOf(false) }
    var playbackPositionMs by remember { mutableIntStateOf(0) }
    var playbackDurationMs by remember { mutableIntStateOf(0) }
    val current = items.getOrNull(pagerState.currentPage)
    val isMotion = current != null && MotionPhotoMuxer.isMotionPhoto(current)
    val isVideo = current?.extension?.equals("mp4", ignoreCase = true) == true
    val formatText = if (current?.extension?.equals("dng", ignoreCase = true) == true) {
        stringResource(R.string.raw_dng)
    } else {
        null
    }
    val remainingNanos = if (isVideo && playbackDurationMs > 0) {
        (playbackDurationMs - playbackPositionMs).coerceAtLeast(0) * 1_000_000L
    } else {
        null
    }
    val metadataLabel = remember(current, formatText, remainingNanos) {
        current?.let { galleryMetadataLabel(it, formatText, remainingNanos) }.orEmpty()
    }

    LaunchedEffect(pagerState.currentPage, videoAutoplay, isVideo) {
        playbackSpeed = 1f
        motionPlaying = false
        videoPlaying = videoAutoplay && isVideo
        videoControlsVisible = true
        showPlayError = false
        playbackPositionMs = 0
        playbackDurationMs = 0
    }
    LaunchedEffect(videoPlaying, videoControlsTick, videoAutoplay) {
        if (!isVideo || videoAutoplay) return@LaunchedEffect
        if (!videoPlaying) {
            videoControlsVisible = true
            return@LaunchedEffect
        }
        videoControlsVisible = true
        delay(2_000)
        videoControlsVisible = false
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
            motionPlaying = motionPlaying,
            videoPlaying = videoPlaying,
            videoAutoplay = videoAutoplay,
            onPlaybackError = {
                motionPlaying = false
                videoPlaying = false
                showPlayError = true
            },
            onPlaybackEnded = {
                if (!videoAutoplay) {
                    videoPlaying = false
                    videoControlsVisible = true
                }
            },
            onVideoTapped = {
                if (!videoAutoplay && videoPlaying) videoControlsTick++
            },
            onPlaybackPosition = { positionMs, durationMs ->
                playbackPositionMs = positionMs
                if (durationMs > 0) playbackDurationMs = durationMs
            }
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
                    modifier = Modifier.padding(bottom = if (isVideo) 12.dp else 8.dp)
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
        if (isVideo && !videoAutoplay) {
            AnimatedVisibility(
                visible = !videoPlaying || videoControlsVisible,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(400)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                CameraGlassButton(
                    icon = if (videoPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (videoPlaying) R.string.pause_video else R.string.play_video
                    ),
                    onClick = {
                        videoPlaying = !videoPlaying
                        if (videoPlaying) videoControlsTick++
                    },
                    diameter = 56.dp,
                    onGlass = chrome.onGlass,
                    glass = chrome.glass,
                    stroke = chrome.stroke
                )
            }
        }
        CameraAlertDialog(
            show = showPlayError,
            title = stringResource(R.string.video_play_error_title),
            text = stringResource(R.string.video_play_error_body),
            confirmLabel = stringResource(R.string.video_play_error_ok),
            onDismiss = { showPlayError = false }
        )
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
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                )
            )
            .padding(start = 20.dp, end = 20.dp, top = 8.dp)
    ) {
        CameraGlassButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back_button_alt),
            onClick = navigateBack,
            onGlass = chrome.onGlass,
            glass = chrome.glass,
            stroke = chrome.stroke
        )
        if (showMotion) {
            val chipShape = RoundedCornerShape(22.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(44.dp)
                    .clip(chipShape)
                    .background(if (motionPlaying) CameraAccent else chrome.glass)
                    .border(1.dp, chrome.stroke, chipShape)
                    .clickable(onClick = onMotionClicked)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(
                        if (motionPlaying) R.string.motion_photo_badge else R.string.play_motion_photo
                    ),
                    color = if (motionPlaying) Color.Black else CameraAccent,
                    fontFamily = CameraMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
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
    CameraAlertDialog(
        show = showDialog.value,
        title = stringResource(R.string.delete_title),
        text = stringResource(R.string.delete_subtitle),
        confirmLabel = stringResource(R.string.delete_button_alt),
        dismissLabel = stringResource(R.string.delete_button_cancel),
        destructiveConfirm = true,
        onConfirm = {
            items.getOrNull(pagerState.currentPage)?.let(onDelete)
        },
        onDismiss = { showDialog.value = false }
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
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
    videoPlaying: Boolean = false,
    videoAutoplay: Boolean = false,
    onPlaybackError: () -> Unit = {},
    onPlaybackEnded: () -> Unit = {},
    onVideoTapped: () -> Unit = {},
    onPlaybackPosition: (positionMs: Int, durationMs: Int) -> Unit = { _, _ -> },
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
                    playing = videoPlaying && pagerState.currentPage == page,
                    playbackSpeed = playbackSpeed,
                    loop = videoAutoplay,
                    onPlaybackError = onPlaybackError,
                    onPlaybackEnded = onPlaybackEnded,
                    onSurfaceTapped = onVideoTapped.takeUnless { videoAutoplay },
                    onPlaybackPosition = onPlaybackPosition
                )
            } else {
                val motion = MotionPhotoMuxer.isMotionPhoto(file)
                val playMotion = motion && motionPlaying && pagerState.currentPage == page
                Box(Modifier.fillMaxSize()) {
                    if (!playMotion) {
                        Image(
                            painter = rememberAsyncImagePainter(model = file),
                            contentScale = ContentScale.Fit,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (playMotion) {
                        GalleryMotionOverlay(file = file, onPlaybackError = onPlaybackError)
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryMotionOverlay(file: File, onPlaybackError: () -> Unit) {
    val context = LocalContext.current
    val clip = remember(file) {
        File(context.cacheDir, "motion_${file.nameWithoutExtension}.mp4")
    }
    var extracted by remember(file) { mutableStateOf<File?>(null) }
    val reportError by rememberUpdatedState(onPlaybackError)
    LaunchedEffect(file) {
        val result = withContext(Dispatchers.IO) {
            runCatching { MotionPhotoMuxer.extractVideo(file, clip) }.getOrNull()
        }
        extracted = result
        if (result == null) reportError()
    }
    val clipFile = extracted
    if (clipFile != null) {
        GalleryVideo(
            file = clipFile,
            isActive = true,
            playing = true,
            playbackSpeed = 1f,
            loop = true,
            onPlaybackError = onPlaybackError
        )
    } else {
        Image(
            painter = rememberAsyncImagePainter(model = file),
            contentScale = ContentScale.Fit,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
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
private fun GalleryVideo(
    file: File,
    isActive: Boolean,
    playing: Boolean,
    playbackSpeed: Float,
    loop: Boolean = false,
    onPlaybackError: () -> Unit = {},
    onPlaybackEnded: () -> Unit = {},
    onSurfaceTapped: (() -> Unit)? = null,
    onPlaybackPosition: (positionMs: Int, durationMs: Int) -> Unit = { _, _ -> }
) {
    val videoView = remember(file) { mutableStateOf<VideoView?>(null) }
    val playerRef = remember(file) { mutableStateOf<android.media.MediaPlayer?>(null) }
    val reportError by rememberUpdatedState(onPlaybackError)
    val reportEnded by rememberUpdatedState(onPlaybackEnded)
    val reportPosition by rememberUpdatedState(onPlaybackPosition)
    val shouldPlay by rememberUpdatedState(isActive && playing)
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    setOnErrorListener { _, _, _ ->
                        reportError()
                        true
                    }
                    setOnCompletionListener {
                        seekTo(1)
                        reportEnded()
                    }
                    setVideoPath(file.absolutePath)
                    setOnPreparedListener { player ->
                        playerRef.value = player
                        player.isLooping = loop
                        runCatching {
                            player.playbackParams = player.playbackParams.setSpeed(playbackSpeed)
                        }
                        if (shouldPlay) {
                            start()
                        } else {
                            seekTo(1)
                        }
                    }
                    videoView.value = this
                }
            },
            update = { view ->
                playerRef.value?.let { player ->
                    player.isLooping = loop
                    runCatching {
                        player.playbackParams = player.playbackParams.setSpeed(playbackSpeed)
                    }
                }
                if (shouldPlay) {
                    if (!view.isPlaying) view.start()
                } else if (view.isPlaying) {
                    view.pause()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (onSurfaceTapped != null) {
            val tap by rememberUpdatedState(onSurfaceTapped)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { tap() }
                    )
            )
        }
    }
    DisposableEffect(file) {
        onDispose {
            videoView.value?.stopPlayback()
            playerRef.value = null
        }
    }
    LaunchedEffect(file, isActive, playing) {
        if (!isActive) return@LaunchedEffect
        while (true) {
            val view = videoView.value
            if (view != null) {
                val duration = view.duration.coerceAtLeast(0)
                val position = view.currentPosition.coerceAtLeast(0)
                reportPosition(position, duration)
            }
            delay(if (playing) 200L else 400L)
        }
    }
}

private fun galleryMetadataLabel(
    file: File,
    formatText: String?,
    remainingNanos: Long? = null
): String {
    val info = mediaFileInfo(file)
    val size = if (info.width > 0 && info.height > 0) "${info.width} × ${info.height}" else null
    val duration = (remainingNanos ?: info.durationNanos)?.let { formatRecordingTime(it) }
    return listOfNotNull(duration, size, formatText).joinToString(" · ")
}
