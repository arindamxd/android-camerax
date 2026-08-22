package com.arindam.camerax.ui.home.camera

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Horizontal mode strip. Settles to a page then calls [onItemSelected]; drag does not
 * rebind the camera until settle.
 */
@Composable
fun <T : Any> DiscretePager(
    items: List<T>,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    initialIndex: Int = 0,
    @FloatRange(from = 0.0, to = 1.0)
    itemFraction: Float = 1f,
    itemWidth: Dp? = null,
    itemSpacing: Dp = 0.dp,
    @FloatRange(from = 0.0, to = 1.0)
    overshootFraction: Float = .5f,
    onItemSelected: (T) -> Unit = {},
    contentFactory: @Composable (item: T, selected: Boolean) -> Unit,
) {
    require(items.isNotEmpty()) { "Pager requires at least one item" }
    require(initialIndex in 0..items.lastIndex) { "Initial index out of bounds" }
    require(itemFraction > 0f && itemFraction <= 1f) { "Item fraction must be in the (0f, 1f] range" }
    require(overshootFraction > 0f && overshootFraction <= 1f) {
        "Overshoot fraction must be in the (0f, 1f] range"
    }
    val scope = rememberCoroutineScope()
    val state = remember(items) { PagerState() }
    state.numberOfItems = items.size
    state.itemFraction = itemFraction
    state.overshootFraction = overshootFraction
    state.itemSpacing = with(LocalDensity.current) { itemSpacing.toPx() }
    state.fixedItemDimension = itemWidth?.let { with(LocalDensity.current) { it.roundToPx() } } ?: 0
    state.orientation = orientation
    state.listener = { index -> onItemSelected(items[index]) }
    state.scope = scope

    Layout(
        content = {
            items.forEachIndexed { index, item ->
                Box(
                    modifier = when (orientation) {
                        Orientation.Horizontal -> Modifier.fillMaxWidth()
                        Orientation.Vertical -> Modifier.fillMaxHeight()
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    contentFactory(item, index == state.currentIndex)
                }
            }
        },
        modifier = modifier
            .clipToBounds()
            .then(state.inputModifier),
    ) { measurable, constraints ->
        val dimension = constraints.dimension(orientation)
        val itemDimension = if (state.fixedItemDimension > 0) {
            state.fixedItemDimension
        } else {
            (dimension * state.itemFraction).roundToInt()
        }
        val looseConstraints = constraints.toLooseConstraints(orientation, itemDimension)
        val placeable = measurable.map { it.measure(looseConstraints) }
        val size = placeable.getSize(orientation, dimension)
        state.itemDimension = itemDimension
        val halfItemDimension = itemDimension / 2
        layout(size.width, size.height) {
            val centerOffset = dimension / 2 - halfItemDimension
            val dragOffset = state.scrollOffset
            val roundedDragOffset = dragOffset.roundToInt()
            val spacing = state.itemSpacing.roundToInt()
            val itemDimensionWithSpace = itemDimension + state.itemSpacing
            val first = ceil(
                x = (dragOffset - itemDimension - centerOffset) / itemDimensionWithSpace
            ).toInt().coerceAtLeast(minimumValue = 0)
            val last = ((dimension + dragOffset - centerOffset) / itemDimensionWithSpace).toInt()
                .coerceAtMost(maximumValue = items.lastIndex)
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

    LaunchedEffect(items, initialIndex) {
        if (!state.isDragging && state.currentIndex != initialIndex) {
            state.snapTo(initialIndex.coerceIn(0, items.lastIndex))
        }
    }
}

private fun Constraints.dimension(orientation: Orientation) = when (orientation) {
    Orientation.Horizontal -> maxWidth
    Orientation.Vertical -> maxHeight
}

private fun Constraints.toLooseConstraints(
    orientation: Orientation,
    itemDimension: Int,
): Constraints {
    return when (orientation) {
        Orientation.Horizontal -> copy(
            minWidth = itemDimension,
            maxWidth = itemDimension,
            minHeight = 0,
        )
        Orientation.Vertical -> copy(
            minWidth = 0,
            minHeight = itemDimension,
            maxHeight = itemDimension,
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
    var fixedItemDimension by mutableIntStateOf(0)
    var orientation by mutableStateOf(Orientation.Horizontal)
    var isDragging by mutableStateOf(false)
    var scope: CoroutineScope? by mutableStateOf(null)
    var listener: (Int) -> Unit by mutableStateOf({})
    var scrollOffset by mutableFloatStateOf(0f)
    private val animator = Animatable(0f)

    private val settleSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    private val tapSpec = tween<Float>(
        durationMillis = 320,
        easing = FastOutSlowInEasing
    )

    private fun step(): Float = (itemDimension + itemSpacing).coerceAtLeast(1f)

    fun indexForOffset(offset: Float): Int =
        (offset / step()).roundToInt()
            .coerceIn(0, (numberOfItems - 1).coerceAtLeast(0))

    fun syncVisualIndex(offset: Float) {
        val index = indexForOffset(offset)
        if (index != currentIndex) currentIndex = index
    }

    suspend fun snapTo(index: Int) {
        val dest = index.coerceIn(0, (numberOfItems - 1).coerceAtLeast(0))
        val target = dest * step()
        animator.snapTo(target)
        scrollOffset = target
        currentIndex = dest
    }

    suspend fun animateTo(index: Int, velocity: Float = 0f) {
        val dest = index.coerceIn(0, (numberOfItems - 1).coerceAtLeast(0))
        val target = dest * step()
        animator.snapTo(scrollOffset)
        animator.animateTo(
            targetValue = target,
            animationSpec = if (velocity.absoluteValue > 80f) settleSpec else tapSpec,
            initialVelocity = velocity
        ) {
            scrollOffset = value
            syncVisualIndex(value)
        }
        scrollOffset = target
        currentIndex = dest
        listener(dest)
    }

    val inputModifier = Modifier.pointerInput(numberOfItems, orientation) {
        fun indexAtPosition(x: Float, y: Float): Int {
            val dimension = when (orientation) {
                Orientation.Horizontal -> size.width
                Orientation.Vertical -> size.height
            }
            val coordinate = when (orientation) {
                Orientation.Horizontal -> x
                Orientation.Vertical -> y
            }
            val centerOffset = dimension / 2f - itemDimension / 2f
            var best = currentIndex
            var bestDistance = Float.MAX_VALUE
            for (index in 0 until numberOfItems) {
                val start = index * (itemDimension + itemSpacing) - scrollOffset + centerOffset
                val center = start + itemDimension / 2f
                val distance = kotlin.math.abs(coordinate - center)
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = index
                }
            }
            return best.coerceIn(0, numberOfItems - 1)
        }

        fun offsetLimit(): OffsetLimit {
            val dimension = when (orientation) {
                Orientation.Horizontal -> size.width
                Orientation.Vertical -> size.height
            }
            val itemSideMargin = (dimension - itemDimension) / 2f
            return OffsetLimit(
                min = -dimension * overshootFraction + itemSideMargin,
                max = numberOfItems * (itemDimension + itemSpacing) -
                    (1f - overshootFraction) * dimension + itemSideMargin,
            )
        }

        awaitEachGesture {
            val tracker = VelocityTracker()
            val decay = splineBasedDecay<Float>(this)
            val down = awaitFirstDown()
            scope?.launch { animator.stop() }
            isDragging = true
            val start = down.position
            var dragged = false
            val slop = viewConfiguration.touchSlop
            val limits = offsetLimit()
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val travel = when (orientation) {
                    Orientation.Horizontal -> kotlin.math.abs(change.position.x - start.x)
                    Orientation.Vertical -> kotlin.math.abs(change.position.y - start.y)
                }
                if (travel > slop) dragged = true
                if (dragged) {
                    val dragChange = change.calculateDragChange(orientation)
                    scrollOffset = (scrollOffset - dragChange).coerceIn(limits.min, limits.max)
                    syncVisualIndex(scrollOffset)
                    if (change.positionChanged()) change.consume()
                    tracker.addPosition(change.uptimeMillis, change.position)
                }
            }
            isDragging = false
            if (!dragged) {
                val tapped = indexAtPosition(start.x, start.y)
                scope?.launch { animateTo(tapped) }
                return@awaitEachGesture
            }
            val velocity = tracker.calculateVelocity(orientation)
            val decayTarget = decay.calculateTargetValue(scrollOffset, -velocity)
            val maxOffset = (numberOfItems - 1).toFloat() * step()
            val projected = decayTarget.coerceIn(0f, maxOffset)
            val nearest = (projected / step()).roundToInt()
                .coerceIn(0, numberOfItems - 1)
            scope?.launch { animateTo(nearest, -velocity) }
        }
    }
}

private data class OffsetLimit(
    val min: Float,
    val max: Float
)

private fun VelocityTracker.calculateVelocity(orientation: Orientation) = when (orientation) {
    Orientation.Horizontal -> calculateVelocity().x
    Orientation.Vertical -> calculateVelocity().y
}

private fun PointerInputChange.calculateDragChange(
    orientation: Orientation
) = when (orientation) {
    Orientation.Horizontal -> positionChange().x
    Orientation.Vertical -> positionChange().y
}
