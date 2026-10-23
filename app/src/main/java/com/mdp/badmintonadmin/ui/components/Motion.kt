package com.mdp.badmintonadmin.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Small, consistent motion vocabulary (same feel as Pickles). Kept deliberately subtle:
 * nothing here delays input, and every animation is display-only.
 */
object Motion {
    const val SHORT = 180
    const val MEDIUM = 280
    val emphasized = FastOutSlowInEasing
}

/** Springy press-down scale for tap targets. Pass the same source to the button. */
fun Modifier.pressScale(source: MutableInteractionSource, pressed: Float = 0.96f): Modifier = composed {
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) pressed else 1f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "press"
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** One-time entrance: fade in and rise a few dp, staggered by [index]. */
fun Modifier.enter(index: Int = 0): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(40L * index)
        progress.animateTo(1f, tween(Motion.MEDIUM + 60, easing = Motion.emphasized))
    }
    val rise = with(LocalDensity.current) { 14.dp.toPx() }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * rise
    }
}

/** Text that rolls (new value drops in from above) whenever [value] changes. */
@Composable
fun <T> Rolling(
    value: T,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            (slideInVertically(tween(Motion.MEDIUM, easing = Motion.emphasized)) { -it / 2 } + fadeIn(tween(Motion.MEDIUM))) togetherWith
                (slideOutVertically(tween(Motion.MEDIUM, easing = Motion.emphasized)) { it / 2 } + fadeOut(tween(Motion.SHORT)))
        },
        contentAlignment = contentAlignment,
        modifier = modifier,
        label = "rolling"
    ) { content(it) }
}
