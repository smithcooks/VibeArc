package com.vibearc.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp

@Composable
private fun glassButtonBorder(): BorderStroke? = if (!LocalGlass.current) null else BorderStroke(.7.dp,
    Brush.linearGradient(listOf(Color.White.copy(alpha = .75f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = .12f), Color.White.copy(alpha = .24f))))

@Composable
internal fun GlassButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = CircleShape, contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    colors: ButtonColors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
    content: @Composable RowScope.() -> Unit) {
    val source = remember { MutableInteractionSource() }
    // Shared tint and highlights, without allocating a live blur layer for every action.
    CompositionLocalProvider(LocalGlassBackdrop provides null) {
        val base = colors.containerColor.takeIf { it.alpha > 0f } ?: MaterialTheme.colorScheme.surface
        androidx.compose.material3.Button(onClick, modifier.clip(shape).glassMaterial(base, true, source),
            enabled = enabled, shape = shape, border = glassButtonBorder(), elevation = null,
            colors = colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
            interactionSource = source, contentPadding = contentPadding, content = content)
    }
}

@Composable
internal fun GlassTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = CircleShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit) =
    GlassButton(onClick, modifier, enabled, shape, contentPadding, colors, content)

@Composable
internal fun GlassIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    CompositionLocalProvider(LocalGlassBackdrop provides null) {
        androidx.compose.material3.IconButton(onClick,
            modifier.clip(CircleShape).glassMaterial(MaterialTheme.colorScheme.surface, true, source),
            enabled = enabled, interactionSource = source, content = content)
    }
}

@Composable
internal fun GlassFilledIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    CompositionLocalProvider(LocalGlassBackdrop provides null) {
        androidx.compose.material3.FilledIconButton(onClick,
            modifier.clip(CircleShape).glassMaterial(MaterialTheme.colorScheme.primary, true, source),
            enabled = enabled, interactionSource = source,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onPrimary), content = content)
    }
}
