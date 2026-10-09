package com.vibearc.app

import android.animation.ValueAnimator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal val LocalMotionEnabled = staticCompositionLocalOf { true }

@Composable
internal fun rememberUiMotionEnabled(reduceMotion: Boolean): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var systemAnimations by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            systemAnimations = ValueAnimator.areAnimatorsEnabled()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return uiMotionEnabled(reduceMotion, systemAnimations, resumed)
}

@Composable
internal fun Modifier.motionPress(source: MutableInteractionSource): Modifier {
    val enabled = LocalMotionEnabled.current
    val pressed by source.collectIsPressedAsState()
    val glass = LocalGlass.current
    val scale = animateFloatAsState(if (pressed && enabled) .965f else 1f,
        if (enabled) spring(dampingRatio = .78f, stiffness = 500f) else snap(), label = "Press feedback")
    return graphicsLayer {
        scaleX = if (glass) 1f - (1f - scale.value) * .65f else scale.value
        scaleY = scale.value
    }
}

@Composable
internal fun Modifier.motionClickable(enabled: Boolean = true, role: Role? = null,
    onClickLabel: String? = null, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return motionPress(source).clickable(interactionSource = source, indication = LocalIndication.current,
        enabled = enabled, role = role, onClickLabel = onClickLabel, onClick = onClick)
}

@Composable
internal fun MotionScene(sceneKey: Any, modifier: Modifier = Modifier, liftDp: Float = 12f,
    scaleFrom: Float = .99f, content: @Composable BoxScope.() -> Unit) {
    val enabled = LocalMotionEnabled.current
    val progress = remember(sceneKey) { Animatable(if (enabled) 0f else 1f) }
    val density = LocalDensity.current.density
    LaunchedEffect(progress, enabled) {
        if (enabled) progress.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
        else progress.snapTo(1f)
    }
    Box(modifier.graphicsLayer {
        val value = if (enabled) progress.value else 1f
        alpha = value
        translationY = (1f - value) * liftDp * density
        scaleX = scaleFrom + (1f - scaleFrom) * value
        scaleY = scaleX
    }) { content() }
}

@Composable
internal fun <T> MotionSwap(target: T, modifier: Modifier = Modifier,
    contentKey: (T) -> Any? = { it }, content: @Composable (T) -> Unit) {
    if (!LocalMotionEnabled.current) Box(modifier) { content(target) }
    else AnimatedContent(targetState = target, modifier = modifier, contentKey = contentKey,
        transitionSpec = {
            ((fadeIn(tween(160)) + scaleIn(tween(220), initialScale = .94f))
                togetherWith fadeOut(tween(90))).using(null)
        }, label = "Content change") { value -> content(value) }
}
