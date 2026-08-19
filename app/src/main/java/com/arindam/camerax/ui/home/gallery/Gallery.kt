@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.arindam.camerax.ui.home.gallery

import android.widget.VideoView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.arindam.camerax.R
import com.arindam.camerax.data.camera.MotionPhotoMuxer
import com.arindam.camerax.ui.compose.DarkLightPreviews
import com.arindam.camerax.ui.theme.AppTheme
import com.arindam.camerax.ui.theme.CameraAccent
import com.arindam.camerax.ui.theme.CameraGlass
import com.arindam.camerax.ui.theme.CameraGlassStrong
import com.arindam.camerax.ui.theme.CameraMono
import com.arindam.camerax.ui.theme.CameraOnGlass
import java.io.File

/**
 * Created by Arindam Karmakar on 18/09/23.
 */

@Composable
fun GalleryScreen(
    dataList: List<File?> = listOf(),
    navigateBack: () -> Unit,
    onShareClicked: (Int) -> Unit
) {
    val mediaList = rememberSaveable { mutableStateOf(listOf<File?>()) }
    val pagerState = rememberPagerState(pageCount = { mediaList.value.size })
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var motionPlaying by remember { mutableStateOf(false) }
    val current = mediaList.value.getOrNull(pagerState.currentPage)
    val isMotion = current != null && MotionPhotoMuxer.isMotionPhoto(current)

    LaunchedEffect(dataList) {
        mediaList.value = dataList.toMutableList()
    }
    LaunchedEffect(pagerState.currentPage) {
        playbackSpeed = 1f
        motionPlaying = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        GalleryPager(
            dataList = mediaList,
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
                        listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
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
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))
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
            if (current?.extension?.equals("mp4", ignoreCase = true) == true) {
                PlaybackSpeedRow(
                    speed = playbackSpeed,
                    onSpeedSelected = { playbackSpeed = it }
                )
            }
            GalleryFooter(
                dataList = mediaList,
                pagerState = pagerState,
                navigateBack = navigateBack,
                onShareClicked = onShareClicked
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
            onShareClicked = {}
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
            .padding(horizontal = 12.dp, vertical = 8.dp)
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
                color = CameraAccent,
                fontFamily = CameraMono,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CameraGlassStrong)
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
    dataList: MutableState<List<File?>>,
    pagerState: PagerState,
    navigateBack: () -> Unit,
    onShareClicked: (Int) -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }
    DeleteDialog(
        show = showDialog.value,
        onConfirmed = {
            val deletedFile = dataList.value[pagerState.currentPage].also { it?.delete() }
            dataList.value = dataList.value.filterNot { it == deletedFile }

            // If all photos have been deleted, return to camera
            if (dataList.value.isEmpty()) navigateBack()
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
        if (dataList.value.isEmpty()) return@Row
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
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(CameraGlassStrong)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = remember { ripple(bounded = false) },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = icon),
            modifier = Modifier.size(22.dp),
            tint = CameraOnGlass,
            contentDescription = contentDescription
        )
    }
}

@Composable
private fun GalleryPager(
    dataList: MutableState<List<File?>>,
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
        dataList.value.getOrNull(page)?.let { file ->
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
                    if (file.extension.equals("dng", ignoreCase = true)) {
                        Text(
                            text = stringResource(R.string.raw_dng),
                            color = CameraAccent,
                            fontFamily = CameraMono,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = 24.dp)
                        )
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CameraGlass)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        speeds.forEach { value ->
            val selected = kotlin.math.abs(speed - value) < 0.01f
            Text(
                text = if (value == 1f) "1x" else "${value}x",
                color = if (selected) Color.Black else Color.White,
                fontFamily = CameraMono,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) CameraAccent else Color.Transparent)
                    .clickable { onSpeedSelected(value) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
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

    AlertDialog(
        onDismissRequest = { onDismiss.invoke() },
        confirmButton = {
            TextButton(onClick = {
                onDismiss.invoke()
                onConfirmed.invoke()
            }) {
                Text(text = stringResource(id = confirmTitle))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss.invoke() }) {
                Text(text = stringResource(id = dismissTitle))
            }
        },
        title = {
            Text(text = stringResource(id = title))
        },
        text = {
            Text(text = stringResource(id = text))
        },
    )
}
