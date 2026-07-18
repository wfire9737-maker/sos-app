package com.example.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.4f),
        Color.LightGray.copy(alpha = 0.15f),
        Color.LightGray.copy(alpha = 0.4f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
    background(brush)
}

@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    borderRadius: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(borderRadius))
            .shimmerEffect()
    )
}

@Composable
fun SkeletonLine(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    widthFraction: Float = 0.7f,
    borderRadius: Dp = 4.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(borderRadius))
            .shimmerEffect()
    )
}

@Composable
fun SkeletonCircle(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .shimmerEffect()
    )
}
