package com.baridhi.drivedash.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baridhi.drivedash.ui.theme.Dash
import kotlin.math.cos
import kotlin.math.sin

private const val START_ANGLE = 150f
private const val SWEEP = 240f

/**
 * Neon arc speedometer.
 * @param speed  current speed in the display unit
 * @param maxScale top of the dial in the display unit
 * @param limit speed limit in the display unit (red marker on the dial)
 */
@Composable
fun Speedometer(
    speed: Float,
    maxScale: Float,
    limit: Float,
    unit: String,
    overSpeed: Boolean,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = speed.coerceIn(0f, maxScale),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow),
        label = "speed"
    )
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow by pulse.animateFloat(
        initialValue = 0.25f, targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "glow"
    )
    val readoutColor by animateColorAsState(if (overSpeed) Dash.Red else Dash.Text, label = "readout")
    val measurer = rememberTextMeasurer()

    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.065f
            val radius = size.minDimension / 2f - stroke
            val c = center
            val topLeft = Offset(c.x - radius, c.y - radius)
            val arcSize = Size(radius * 2, radius * 2)

            // Soft halo behind the dial; pulses red when over the limit
            val haloColor = if (overSpeed) Dash.Red.copy(alpha = glow * 0.45f) else Dash.Cyan.copy(alpha = 0.12f)
            drawCircle(
                brush = Brush.radialGradient(listOf(haloColor, Color.Transparent), center = c, radius = radius * 1.2f),
                radius = radius * 1.2f, center = c
            )

            // Track
            drawArc(
                color = Color(0x1FFFFFFF), startAngle = START_ANGLE, sweepAngle = SWEEP, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
            )

            // Value arc: cyan -> lime -> amber -> magenta/red along the dial
            val sweep = SWEEP * (animated / maxScale)
            if (sweep > 0.5f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to Dash.Magenta,
                        0.083f to Dash.Red,
                        0.417f to Dash.Cyan,
                        0.6f to Dash.Lime,
                        0.78f to Dash.Amber,
                        0.92f to Dash.Magenta,
                        1f to Dash.Magenta,
                        center = c
                    ),
                    startAngle = START_ANGLE, sweepAngle = sweep, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }

            // Ticks and numbers
            val majorStep = 20f
            var v = 0f
            while (v <= maxScale + 0.01f) {
                val a = Math.toRadians((START_ANGLE + SWEEP * v / maxScale).toDouble())
                val cosA = cos(a).toFloat()
                val sinA = sin(a).toFloat()
                val isMajor = v % majorStep == 0f
                val outer = radius - stroke * 1.0f
                val inner = outer - if (isMajor) stroke * 0.8f else stroke * 0.4f
                val passed = v <= animated
                drawLine(
                    color = when {
                        passed && isMajor -> Color.White
                        isMajor -> Color(0x99FFFFFF)
                        else -> Color(0x44FFFFFF)
                    },
                    start = Offset(c.x + inner * cosA, c.y + inner * sinA),
                    end = Offset(c.x + outer * cosA, c.y + outer * sinA),
                    strokeWidth = if (isMajor) 5f else 2.5f,
                    cap = StrokeCap.Round
                )
                if (isMajor) {
                    val layout = measurer.measure(
                        v.toInt().toString(),
                        TextStyle(
                            color = if (passed) Color.White else Color(0x88FFFFFF),
                            fontSize = 12.sp, fontWeight = FontWeight.Medium
                        )
                    )
                    val lr = inner - stroke * 0.9f
                    drawText(
                        layout,
                        topLeft = Offset(
                            c.x + lr * cosA - layout.size.width / 2f,
                            c.y + lr * sinA - layout.size.height / 2f
                        )
                    )
                }
                v += 10f
            }

            // Speed limit marker
            if (limit in 0f..maxScale) {
                val la = Math.toRadians((START_ANGLE + SWEEP * limit / maxScale).toDouble())
                val r1 = radius + stroke * 0.75f
                val r2 = radius - stroke * 0.75f
                drawLine(
                    color = Dash.Red,
                    start = Offset(c.x + r1 * cos(la).toFloat(), c.y + r1 * sin(la).toFloat()),
                    end = Offset(c.x + r2 * cos(la).toFloat(), c.y + r2 * sin(la).toFloat()),
                    strokeWidth = 7f, cap = StrokeCap.Round
                )
            }

            // Pointer: a floating blade that leaves the centre free for the readout
            val na = Math.toRadians((START_ANGLE + sweep).toDouble())
            val nc = cos(na).toFloat()
            val ns = sin(na).toFloat()
            val tip = Offset(c.x + (radius - stroke * 1.9f) * nc, c.y + (radius - stroke * 1.9f) * ns)
            val tail = Offset(c.x + radius * 0.52f * nc, c.y + radius * 0.52f * ns)
            val pointerColor = if (overSpeed) Dash.Red else Dash.Cyan
            drawLine(
                brush = Brush.linearGradient(listOf(Color.Transparent, pointerColor, Color.White), start = tail, end = tip),
                start = tail, end = tip, strokeWidth = stroke * 0.32f, cap = StrokeCap.Round
            )
            drawCircle(pointerColor.copy(alpha = 0.35f), radius = stroke * 0.55f, center = tip)
            drawCircle(Color.White, radius = stroke * 0.22f, center = tip)
        }

        // Digital readout
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = speed.toInt().toString(),
                color = readoutColor,
                fontSize = 76.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp
            )
            Text(unit, color = Dash.TextDim, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (overSpeed) Dash.Red else Color(0x1AFFFFFF))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (overSpeed) Color.White else Dash.Red))
                Text(
                    if (overSpeed) "Slow down" else "Limit ${limit.toInt()}",
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
