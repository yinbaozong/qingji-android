package com.dreamjournal.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.background,
                        if (dark) Color(0xFF16201E) else Color(0xFFE7EEEA),
                        colors.background
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.primary.copy(alpha = if (dark) 0.14f else 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.16f),
                    radius = size.width * 0.82f
                ),
                radius = size.width * 0.82f,
                center = Offset(size.width * 0.12f, size.height * 0.16f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.secondary.copy(alpha = if (dark) 0.12f else 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.54f),
                    radius = size.width * 0.90f
                ),
                radius = size.width * 0.90f,
                center = Offset(size.width * 0.92f, size.height * 0.54f)
            )
        }
        content()
    }
}

@Composable
fun Modifier.glassSurface(
    radius: Dp = 20.dp,
    alpha: Float = 0.72f
): Modifier {
    val shape = RoundedCornerShape(radius)
    val surface = MaterialTheme.colorScheme.surface
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return this
        .shadow(
            elevation = 18.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = if (dark) 0.24f else 0.08f),
            spotColor = Color.Black.copy(alpha = if (dark) 0.30f else 0.10f)
        )
        .background(
            brush = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = if (dark) 0.08f else 0.46f),
                    surface.copy(alpha = alpha + if (dark) 0.04f else 0.10f),
                    surface.copy(alpha = alpha.coerceAtMost(0.78f))
                )
            ),
            shape = shape
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = if (dark) 0.20f else 0.72f),
                    MaterialTheme.colorScheme.outline.copy(alpha = if (dark) 0.24f else 0.30f)
                )
            ),
            shape = shape
        )
}

@Composable
fun glassCardColor(alpha: Float = 0.72f): Color {
    return MaterialTheme.colorScheme.surface.copy(alpha = alpha)
}
