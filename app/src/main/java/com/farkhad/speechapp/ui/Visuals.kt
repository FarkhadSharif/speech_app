package com.farkhad.speechapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun NetworkImageBackground(
    url: String,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    overlayColor: Color = Color.Transparent
) {
    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = alpha
        )
        if (overlayColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor)
            )
        }
    }
}

@Composable
fun KazakhPatternBackground(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.2f),
    strokeWidth: Float = 1.5f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val step = 200.dp.toPx()

        for (x in -step.toInt()..width.toInt() step step.toInt()) {
            for (y in -step.toInt()..height.toInt() step step.toInt()) {
                drawQoshqarMuyiz(x.toFloat(), y.toFloat(), step * 0.4f, color, strokeWidth)
            }
        }
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawQoshqarMuyiz(
    x: Float,
    y: Float,
    size: Float,
    color: Color,
    strokeWidth: Float
) {
    val path = Path().apply {
        moveTo(x + size * 0.5f, y + size * 0.8f)
        cubicTo(x + size * 0.1f, y + size * 0.8f, x + size * 0.05f, y + size * 0.4f, x + size * 0.3f, y + size * 0.2f)
        cubicTo(x + size * 0.4f, y + size * 0.1f, x + size * 0.6f, y + size * 0.1f, x + size * 0.7f, y + size * 0.2f)
        cubicTo(x + size * 0.95f, y + size * 0.4f, x + size * 0.9f, y + size * 0.8f, x + size * 0.5f, y + size * 0.8f)
        moveTo(x + size * 0.3f, y + size * 0.2f)
        cubicTo(x + size * 0.4f, y + size * 0.3f, x + size * 0.6f, y + size * 0.3f, x + size * 0.7f, y + size * 0.2f)
    }
    drawPath(path, color, style = Stroke(width = strokeWidth))
}

@Composable
fun SteppeBackground(modifier: Modifier = Modifier) {
    NetworkImageBackground(
        url = "https://images.unsplash.com/photo-1542332213-9b5a5a3fad35?auto=format&fit=crop&q=80&w=1000",
        modifier = modifier
    )
}
