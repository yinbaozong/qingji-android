package com.dreamjournal.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dreamjournal.app.ui.theme.BrandPaper

@Composable
fun QingJiLogo(size: Dp = 48.dp) {
    val shape = RoundedCornerShape(size * 0.27f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF10272F), Color(0xFF203845), Color(0xFF16272F))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val leftPage = Path().apply {
                moveTo(w * 0.13f, h * 0.29f)
                quadraticTo(w * 0.15f, h * 0.18f, w * 0.28f, h * 0.21f)
                lineTo(w * 0.49f, h * 0.31f)
                lineTo(w * 0.49f, h * 0.79f)
                quadraticTo(w * 0.31f, h * 0.68f, w * 0.13f, h * 0.73f)
                close()
            }
            val rightPage = Path().apply {
                moveTo(w * 0.87f, h * 0.29f)
                quadraticTo(w * 0.85f, h * 0.18f, w * 0.72f, h * 0.21f)
                lineTo(w * 0.51f, h * 0.31f)
                lineTo(w * 0.51f, h * 0.79f)
                quadraticTo(w * 0.69f, h * 0.68f, w * 0.87f, h * 0.73f)
                close()
            }

            drawPath(
                path = leftPage,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF9FE5E1), Color(0xFF5A8FD4), Color(0xFF7B6DB5)),
                    start = Offset(w * 0.10f, h * 0.18f),
                    end = Offset(w * 0.52f, h * 0.80f)
                )
            )
            drawPath(
                path = rightPage,
                brush = Brush.linearGradient(
                    listOf(Color(0xFFFFD7A3), Color(0xFFF38B78), Color(0xFFD35F78)),
                    start = Offset(w * 0.52f, h * 0.18f),
                    end = Offset(w * 0.90f, h * 0.80f)
                )
            )
            drawPath(leftPage, Color.White.copy(alpha = 0.40f), style = Stroke(w * 0.018f))
            drawPath(rightPage, Color.White.copy(alpha = 0.42f), style = Stroke(w * 0.018f))
            drawLine(
                color = BrandPaper.copy(alpha = 0.85f),
                start = Offset(w * 0.50f, h * 0.31f),
                end = Offset(w * 0.50f, h * 0.79f),
                strokeWidth = w * 0.025f
            )
            drawCircle(Color(0xFF17353E), radius = w * 0.070f, center = Offset(w * 0.50f, h * 0.64f))
            drawCircle(Color.White, radius = w * 0.028f, center = Offset(w * 0.50f, h * 0.64f))
        }
    }
}

@Composable
fun QingJiBrandTitle(
    title: String = "瞬记",
    subtitle: String = "把生活留在此刻",
    compact: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QingJiLogo(size = if (compact) 38.dp else 48.dp)
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
