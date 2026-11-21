package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalNuxColors
import com.example.ui.theme.NuxBrandEnd
import com.example.ui.theme.NuxBrandMid
import com.example.ui.theme.NuxBrandStart
import com.example.ui.theme.NuxGradientTextStart
import com.example.viewmodel.NuxToast
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NuxAtomLogo(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    withGlow: Boolean = true
) {
    val infinite = rememberInfiniteTransition(label = "nux_atom")
    val globalAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(36_000, easing = LinearEasing)),
        label = "global_orbit"
    )
    val e1 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(5500, easing = LinearEasing)),
        label = "e1"
    )
    val e2 by infinite.animateFloat(
        initialValue = 1.5f,
        targetValue = (2 * PI + 1.5).toFloat(),
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "e2"
    )
    val e3 by infinite.animateFloat(
        initialValue = 3.2f,
        targetValue = (2 * PI + 3.2).toFloat(),
        animationSpec = infiniteRepeatable(tween(6800, easing = LinearEasing)),
        label = "e3"
    )

    val rot = if (animated) globalAngle else 0f

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = minOf(cx, cy)

        if (withGlow) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NuxBrandStart.copy(alpha = 0.28f),
                        NuxBrandEnd.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = r * 1.1f
                ),
                radius = r * 1.1f,
                center = Offset(cx, cy)
            )
        }

        val orbitRx = r * 0.84f
        val orbitRy = r * 0.34f
        val strokeW = (r * 0.085f).coerceAtLeast(1.8f)

        val orbitAngles = listOf(0f, 60f, -60f)
        val orbitColors = listOf(NuxGradientTextStart, NuxBrandMid, NuxBrandEnd)
        val electronPhases = listOf(e1, e2, e3)

        orbitAngles.forEachIndexed { idx, deg ->
            rotate(degrees = deg + rot, pivot = Offset(cx, cy)) {
                drawOval(
                    color = orbitColors[idx].copy(alpha = 0.88f),
                    topLeft = Offset(cx - orbitRx, cy - orbitRy),
                    size = Size(orbitRx * 2f, orbitRy * 2f),
                    style = Stroke(width = strokeW)
                )
                val phase = if (animated) electronPhases[idx] else (idx * 2.1f)
                val ex = cx + orbitRx * cos(phase)
                val ey = cy + orbitRy * sin(phase)
                drawCircle(
                    color = Color.White,
                    radius = strokeW * 1.15f,
                    center = Offset(ex, ey)
                )
                drawCircle(
                    color = orbitColors[idx],
                    radius = strokeW * 0.72f,
                    center = Offset(ex, ey)
                )
            }
        }

        drawCircle(
            brush = Brush.linearGradient(listOf(NuxBrandStart, NuxBrandEnd)),
            radius = r * 0.22f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.92f),
            radius = r * 0.11f,
            center = Offset(cx, cy)
        )
    }
}

@Composable
fun ChatBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val nux = LocalNuxColors.current
    val bgColor = MaterialTheme.colorScheme.background
    val infinite = rememberInfiniteTransition(label = "backdrop")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(26_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_drift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .drawBehind {
                val w = size.width
                val h = size.height
                val alpha1 = if (nux.isDark) 0.085f else 0.06f
                val alpha2 = if (nux.isDark) 0.07f else 0.045f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NuxBrandStart.copy(alpha = alpha1), Color.Transparent),
                        center = Offset(w * (0.22f + drift * 0.15f), h * (0.18f + drift * 0.08f)),
                        radius = w * 0.85f
                    ),
                    radius = w * 0.85f,
                    center = Offset(w * (0.22f + drift * 0.15f), h * (0.18f + drift * 0.08f))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NuxBrandEnd.copy(alpha = alpha2), Color.Transparent),
                        center = Offset(w * (0.82f - drift * 0.18f), h * (0.76f - drift * 0.10f)),
                        radius = w * 0.9f
                    ),
                    radius = w * 0.9f,
                    center = Offset(w * (0.82f - drift * 0.18f), h * (0.76f - drift * 0.10f))
                )
            },
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    strong: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val nux = LocalNuxColors.current
    val shape = RoundedCornerShape(cornerRadius)
    val surfaceColor = if (strong) nux.glassSurfaceStrong else nux.glassSurface

    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = surfaceColor,
        border = BorderStroke(1.dp, nux.glassBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun BrandGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 46.dp,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val nux = LocalNuxColors.current
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(
                brush = if (enabled) nux.brandGradient else Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VoiceBarsIcon(
    isAnimating: Boolean,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "voice_bars")
    val b1 by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(760), RepeatMode.Reverse),
        label = "b1"
    )
    val b2 by infinite.animateFloat(
        initialValue = 0.90f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(tween(920), RepeatMode.Reverse),
        label = "b2"
    )
    val b3 by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(840), RepeatMode.Reverse),
        label = "b3"
    )
    val b4 by infinite.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(tween(1040), RepeatMode.Reverse),
        label = "b4"
    )

    val heights = if (isAnimating) listOf(b1, b2, b3, b4) else listOf(0.45f, 0.85f, 0.65f, 0.40f)

    Canvas(modifier = modifier.size(18.dp)) {
        val barWidth = size.width / 7f
        val gap = (size.width - barWidth * 4f) / 3f
        heights.forEachIndexed { idx, frac ->
            val h = size.height * frac
            val x = idx * (barWidth + gap) + barWidth / 2f
            val yTop = (size.height - h) / 2f
            drawLine(
                color = color,
                start = Offset(x, yTop),
                end = Offset(x, yTop + h),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun WaterVoiceOrb(
    isListening: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "water_orb")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = if (isSpeaking) 1.12f else if (isListening) 1.06f else 1.0f,
        animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
        label = "orb_pulse"
    )
    val halo by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "orb_halo"
    )

    Canvas(modifier = modifier.size(180.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseR = size.minDimension * 0.36f * pulse

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NuxBrandStart.copy(alpha = halo),
                    NuxBrandEnd.copy(alpha = halo * 0.5f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = baseR * 1.65f
            ),
            radius = baseR * 1.65f,
            center = Offset(cx, cy)
        )

        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(NuxGradientTextStart, NuxBrandStart, NuxBrandEnd)
            ),
            radius = baseR,
            center = Offset(cx, cy)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(cx - baseR * 0.28f, cy - baseR * 0.28f),
                radius = baseR * 0.7f
            ),
            radius = baseR * 0.92f,
            center = Offset(cx, cy)
        )
    }
}

@Composable
fun TypingDotsIndicator(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "typing")
    val phases = List(3) { idx ->
        infinite.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(550, delayMillis = idx * 150),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_$idx"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        phases.forEach { alphaState ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alphaState.value))
            )
        }
    }
}

@Composable
fun NuxToastBanner(
    toast: NuxToast?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nux = LocalNuxColors.current
    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (toast != null) {
            val borderCol = if (toast.isError) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else nux.glassBorder
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(nux.glassSurfaceStrong)
                    .border(1.dp, borderCol, RoundedCornerShape(999.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NuxAtomLogo(size = 22.dp, animated = false, withGlow = false)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = toast.message,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (toast.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
