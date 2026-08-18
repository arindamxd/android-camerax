package com.arindam.camerax.ui.home.camera

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.verticalDrag
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
import kotlin.math.sign

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
    val state = remember(items) { PagerState() }
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
                val start = index * (itemDimension + itemSpacing) - dragOffset.value + centerOffset
                val center = start + itemDimension / 2f
                val distance = kotlin.math.abs(coordinate - center)
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = index
                }
            }
            return best.coerceIn(0, numberOfItems - 1)
        }

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
            val start = down.position
            var dragged = false
            val slop = viewConfiguration.touchSlop
            val offsetLimit = calculateOffsetLimit()
            val dragHandler = { change: PointerInputChange ->
                val travel = when (orientation) {
                    Orientation.Horizontal -> kotlin.math.abs(change.position.x - start.x)
                    Orientation.Vertical -> kotlin.math.abs(change.position.y - start.y)
                }
                if (travel > slop) dragged = true
                if (dragged) {
                    scope?.launch {
                        val dragChange = change.calculateDragChange(orientation)
                        dragOffset.snapTo(
                            (dragOffset.value - dragChange).coerceIn(offsetLimit.min, offsetLimit.max)
                        )
                        updateIndex(dragOffset.value)
                    }
                    tracker.addPosition(change.uptimeMillis, change.position)
                }
            }
            when (orientation) {
                Orientation.Horizontal -> horizontalDrag(down.id, dragHandler)
                Orientation.Vertical -> verticalDrag(down.id, dragHandler)
            }
            if (!dragged) {
                val tapped = indexAtPosition(start.x, start.y)
                scope?.launch {
                    snapTo(tapped)
                    updateIndex(dragOffset.value)
                }
                return@awaitEachGesture
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

private data class OffsetLimit(
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
