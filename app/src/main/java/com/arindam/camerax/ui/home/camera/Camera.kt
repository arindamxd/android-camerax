package com.arindam.camerax.ui.home.camera

import android.graphics.drawable.ColorDrawable
import android.util.Log
import androidx.annotation.FloatRange
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toFile
import androidx.lifecycle.lifecycleScope
import coil.compose.rememberAsyncImagePainter
import com.arindam.camerax.R
import com.arindam.camerax.ui.compose.DarkLightPreviews
import com.arindam.camerax.ui.theme.AppTheme
import com.arindam.camerax.util.ANIMATION_FAST_MILLIS
import com.arindam.camerax.util.ANIMATION_SLOW_MILLIS
import com.arindam.camerax.util.commons.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Created by Arindam Karmakar on 18/09/23.
 */

enum class CameraMode {
    PHOTO,
    VIDEO,
    FILTER
}

enum class CameraState(val selector: CameraSelector) {
    BACK(CameraSelector.DEFAULT_BACK_CAMERA),
    FRONT(CameraSelector.DEFAULT_FRONT_CAMERA)
}

@Composable
fun CameraScreen(
    baseFolder: File?,
    onGalleryClicked: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val galleryThumb = remember { mutableStateOf<File?>(null) }
    val cameraMode = rememberSaveable { mutableStateOf(CameraMode.PHOTO) }
    val cameraState = rememberSaveable { mutableStateOf(CameraState.BACK) }

    val previewView: PreviewView = remember { PreviewView(context) }
    val imageCapture: MutableState<ImageCapture?> = remember { mutableStateOf(null) }
    val videoCapture: MutableState<VideoCapture<Recorder>?> = remember { mutableStateOf(null) }

    LaunchedEffect(previewView) {
        when (cameraMode.value) {
            CameraMode.PHOTO -> imageCapture.value = context.getImageCaptureUseCase(
                lifecycleOwner = lifecycleOwner,
                cameraSelector = cameraState.value.selector,
                previewView = previewView
            )
            CameraMode.VIDEO -> videoCapture.value = context.getVideoCaptureUseCase(
                lifecycleOwner = lifecycleOwner,
                cameraSelector = cameraState.value.selector,
                previewView = previewView
            )
            CameraMode.FILTER -> {
                // TODO
            }
        }

        baseFolder?.listFiles { file ->
            Constants.FILE.EXTENSION_WHITELIST.contains(file.extension.lowercase(Locale.US))
        }?.maxOrNull()?.let { galleryThumb.value = it }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )

    Box(
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .height(120.dp)
                .background(color = colorResource(id = R.color.black_900_alpha_020))
        ) {
            DiscretePager(
                items = CameraMode.entries,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                itemFraction = .20f,
                overshootFraction = .75f,
                initialIndex = CameraMode.PHOTO.ordinal,
                itemSpacing = 15.dp,
                onItemSelected = { cameraMode.value = it },
                contentFactory = { item ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorResource(id = R.color.orange_500)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = R.drawable.ic_switch),
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier
                            .size(45.dp)
                            .align(Alignment.Center)
                            .clickable {
                                cameraState.value = when (cameraState.value) {
                                    CameraState.FRONT -> CameraState.BACK
                                    CameraState.BACK -> CameraState.FRONT
                                }

                                lifecycleOwner.lifecycleScope.launch {
                                    when (cameraMode.value) {
                                        CameraMode.PHOTO -> imageCapture.value = context.getImageCaptureUseCase(
                                            lifecycleOwner = lifecycleOwner,
                                            cameraSelector = cameraState.value.selector,
                                            previewView = previewView
                                        )
                                        CameraMode.VIDEO -> videoCapture.value = context.getVideoCaptureUseCase(
                                            lifecycleOwner = lifecycleOwner,
                                            cameraSelector = cameraState.value.selector,
                                            previewView = previewView
                                        )
                                        CameraMode.FILTER -> {
                                            // TODO
                                        }
                                    }
                                }
                            }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = getIconByCameraMode(cameraMode.value, isSystemInDarkTheme())
                        ),
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .align(Alignment.Center)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(bounded = false)
                            ) {
                                when (cameraMode.value) {
                                    CameraMode.PHOTO -> imageCapture.value?.let { capture ->
                                        takePhoto(baseFolder, capture) {
                                            galleryThumb.value = it
                                        }

                                        // Display flash animation to indicate that photo was captured
                                        executeFlash(previewView)
                                    }
                                    CameraMode.VIDEO -> {

                                    }
                                    CameraMode.FILTER -> {
                                        // TODO
                                    }
                                }
                            }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    val padding = if (galleryThumb.value == null) 10.dp else 0.dp
                    Image(
                        painter = rememberAsyncImagePainter(model = galleryThumb.value ?: R.drawable.ic_photo),
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier
                            .size(45.dp)
                            .clip(CircleShape)
                            .align(Alignment.Center)
                            .border((2.5).dp, Color.White, CircleShape)
                            .padding(padding)
                            .clickable {
                                onGalleryClicked()
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@DarkLightPreviews
@Composable
private fun CameraScreenPreview() {
    AppTheme {
        CameraScreen(
            null,
            onGalleryClicked = {}
        )
    }
}

@Composable
fun <T : Any> DiscretePager(
    items: List<T>,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    initialIndex: Int = 0,
    @FloatRange(from = 0.0, to = 1.0)
    itemFraction: Float = 1f,
    itemSpacing: Dp = 0.dp,
    @FloatRange(from = 0.0, to = 1.0)
    overshootFraction: Float = .5f,
    onItemSelected: (T) -> Unit = {},
    contentFactory: @Composable (T) -> Unit,
) {
    require(initialIndex in 0..items.lastIndex) { "Initial index out of bounds" }
    require(itemFraction > 0f && itemFraction <= 1f) { "Item fraction must be in the (0f, 1f] range" }
    require(overshootFraction > 0f && itemFraction <= 1f) { "Overshoot fraction must be in the (0f, 1f] range" }
    val scope = rememberCoroutineScope()
    val state = rememberPagerState()
    state.currentIndex = initialIndex
    state.numberOfItems = items.size
    state.itemFraction = itemFraction
    state.overshootFraction = overshootFraction
    state.itemSpacing = with(LocalDensity.current) { itemSpacing.toPx() }
    state.orientation = orientation
    state.listener = { index -> onItemSelected(items[index]) }
    state.scope = scope

    Layout(
        content = {
            items.map { item ->
                Box(
                    modifier = when (orientation) {
                        Orientation.Horizontal -> Modifier.fillMaxWidth()
                        Orientation.Vertical -> Modifier.fillMaxHeight()
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    contentFactory(item)
                }
            }
        },
        modifier = modifier
            .clipToBounds()
            .then(state.inputModifier),
    ) { measurable, constraints ->
        val dimension = constraints.dimension(orientation)
        val looseConstraints = constraints.toLooseConstraints(orientation, state.itemFraction)
        val placeable = measurable.map { it.measure(looseConstraints) }
        val size = placeable.getSize(orientation, dimension)
        val itemDimension = (dimension * state.itemFraction).roundToInt()
        state.itemDimension = itemDimension
        val halfItemDimension = itemDimension / 2
        layout(size.width, size.height) {
            val centerOffset = dimension / 2 - halfItemDimension
            val dragOffset = state.dragOffset.value
            val roundedDragOffset = dragOffset.roundToInt()
            val spacing = state.itemSpacing.roundToInt()
            val itemDimensionWithSpace = itemDimension + state.itemSpacing
            val first = ceil(x = (dragOffset - itemDimension - centerOffset) / itemDimensionWithSpace).toInt().coerceAtLeast(minimumValue = 0)
            val last = ((dimension + dragOffset - centerOffset) / itemDimensionWithSpace).toInt().coerceAtMost(maximumValue = items.lastIndex)
            for (i in first..last) {
                val offset = i * (itemDimension + spacing) - roundedDragOffset + centerOffset
                placeable[i].place(
                    x = when (orientation) {
                        Orientation.Horizontal -> offset
                        Orientation.Vertical -> 0
                    },
                    y = when (orientation) {
                        Orientation.Horizontal -> 0
                        Orientation.Vertical -> offset
                    }
                )
            }
        }
    }

    LaunchedEffect(key1 = items, key2 = initialIndex) {
        state.snapTo(initialIndex)
    }
}

@Composable
private fun rememberPagerState(): PagerState = remember { PagerState() }

private fun Constraints.dimension(orientation: Orientation) = when (orientation) {
    Orientation.Horizontal -> maxWidth
    Orientation.Vertical -> maxHeight
}

private fun Constraints.toLooseConstraints(
    orientation: Orientation,
    itemFraction: Float,
): Constraints {
    val dimension = dimension(orientation)
    return when (orientation) {
        Orientation.Horizontal -> copy(
            minWidth = (dimension * itemFraction).roundToInt(),
            maxWidth = (dimension * itemFraction).roundToInt(),
            minHeight = 0,
        )
        Orientation.Vertical -> copy(
            minWidth = 0,
            minHeight = (dimension * itemFraction).roundToInt(),
            maxHeight = (dimension * itemFraction).roundToInt(),
        )
    }
}

private fun List<Placeable>.getSize(
    orientation: Orientation,
    dimension: Int,
): IntSize {
    return when (orientation) {
        Orientation.Horizontal -> IntSize(
            width = dimension,
            height = maxByOrNull { it.height }?.height ?: 0
        )
        Orientation.Vertical -> IntSize(
            width = maxByOrNull { it.width }?.width ?: 0,
            height = dimension
        )
    }
}

private class PagerState {
    var currentIndex by mutableIntStateOf(0)
    var numberOfItems by mutableIntStateOf(0)
    var itemFraction by mutableFloatStateOf(0f)
    var overshootFraction by mutableFloatStateOf(0f)
    var itemSpacing by mutableFloatStateOf(0f)
    var itemDimension by mutableIntStateOf(0)
    var orientation by mutableStateOf(Orientation.Horizontal)
    var scope: CoroutineScope? by mutableStateOf(null)
    var listener: (Int) -> Unit by mutableStateOf({})
    val dragOffset = Animatable(0f)

    private val animationSpec = SpringSpec<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow,
    )

    suspend fun snapTo(index: Int) {
        dragOffset.snapTo(index.toFloat() * (itemDimension + itemSpacing))
    }

    val inputModifier = Modifier.pointerInput(numberOfItems) {
        fun itemIndex(offset: Int): Int = (offset / (itemDimension + itemSpacing))
            .roundToInt()
            .coerceIn(0, numberOfItems - 1)

        fun updateIndex(offset: Float) {
            val index = itemIndex(offset.roundToInt())
            if (index != currentIndex) {
                currentIndex = index
                listener(index)
            }
        }

        fun calculateOffsetLimit(): OffsetLimit {
            val dimension = when (orientation) {
                Orientation.Horizontal -> size.width
                Orientation.Vertical -> size.height
            }
            val itemSideMargin = (dimension - itemDimension) / 2f
            return OffsetLimit(
                min = -dimension * overshootFraction + itemSideMargin,
                max = numberOfItems * (itemDimension + itemSpacing) - (1f - overshootFraction) * dimension + itemSideMargin,
            )
        }

        awaitEachGesture {
            val tracker = VelocityTracker()
            val decay = splineBasedDecay<Float>(this)
            val down = awaitFirstDown()
            val offsetLimit = calculateOffsetLimit()
            val dragHandler = { change: PointerInputChange ->
                scope?.launch {
                    val dragChange = change.calculateDragChange(orientation)
                    dragOffset.snapTo((dragOffset.value - dragChange).coerceIn(offsetLimit.min, offsetLimit.max))
                    updateIndex(dragOffset.value)
                }
                tracker.addPosition(change.uptimeMillis, change.position)
            }
            when (orientation) {
                Orientation.Horizontal -> horizontalDrag(down.id, dragHandler)
                Orientation.Vertical -> verticalDrag(down.id, dragHandler)
            }
            val velocity = tracker.calculateVelocity(orientation)
            scope?.launch {
                var targetOffset = decay.calculateTargetValue(dragOffset.value, -velocity)
                val remainder = targetOffset.toInt().absoluteValue % itemDimension
                val extra = if (remainder > itemDimension / 2f) 1 else 0
                val lastVisibleIndex = (targetOffset.absoluteValue / itemDimension.toFloat()).toInt() + extra
                targetOffset = (lastVisibleIndex * (itemDimension + itemSpacing) * targetOffset.sign)
                    .coerceIn(0f, (numberOfItems - 1).toFloat() * (itemDimension + itemSpacing))
                dragOffset.animateTo(
                    animationSpec = animationSpec,
                    targetValue = targetOffset,
                    initialVelocity = -velocity
                ) {
                    updateIndex(value)
                }
            }
        }
    }
}

data class OffsetLimit(
    val min: Float,
    val max: Float
)

private fun VelocityTracker.calculateVelocity(orientation: Orientation) = when (orientation) {
    Orientation.Horizontal -> calculateVelocity().x
    Orientation.Vertical -> calculateVelocity().y
}

private fun PointerInputChange.calculateDragChange(orientation: Orientation) = when (orientation) {
    Orientation.Horizontal -> positionChange().x
    Orientation.Vertical -> positionChange().y
}

fun getPhotoFile(baseFolder: File?): File? {
    val fileName = "yyyy-MM-dd-HH-mm-ss-SSS"
    val extension = ".jpg"
    return File(baseFolder, SimpleDateFormat(fileName, Locale.US).format(System.currentTimeMillis()) + extension)
}

fun getIconByCameraMode(cameraMode: CameraMode, isDark: Boolean): Any = when (cameraMode) {
    CameraMode.PHOTO -> if (isDark) R.drawable.ic_camera_photo_light else R.drawable.ic_camera_photo_dark
    CameraMode.VIDEO -> if (isDark) R.drawable.ic_camera_video_light else R.drawable.ic_camera_video_dark
    CameraMode.FILTER -> R.drawable.ic_open_source
}

fun takePhoto(baseFolder: File?, capture: ImageCapture, onSaved: (File) -> Unit) {
    val photoFile = getPhotoFile(baseFolder)
    photoFile?.let {
        val outputOptions = ImageCapture.OutputFileOptions.Builder(it).build()
        capture.takePicture(outputOptions, Executors.newSingleThreadExecutor(), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val captureFile: File = output.savedUri?.toFile() ?: it
                onSaved.invoke(captureFile)
                Log.d("XX", "Photo capture succeeded: $captureFile")
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("XX", "Photo capture exception: $exception")
            }
        })
    }
}

fun executeFlash(view: PreviewView) {
    view.postDelayed({
        view.foreground = ColorDrawable(Color.Black.hashCode())
        view.postDelayed({ view.foreground = null }, ANIMATION_FAST_MILLIS)
    }, ANIMATION_SLOW_MILLIS)
}
