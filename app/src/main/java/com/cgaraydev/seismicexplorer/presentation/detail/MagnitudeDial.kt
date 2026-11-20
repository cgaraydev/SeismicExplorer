package com.cgaraydev.seismicexplorer.presentation.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cgaraydev.seismicexplorer.R
import kotlin.math.min

/**
 * Arte circular no proporcional: los 12 colores NO representan riesgo, MMI ni daños.
 * El dato de magnitud sigue siendo texto real y el usuario puede acceder al contenido educativo.
 *
 * Componente visual derivado del piloto editable MagnitudeDial de Figma.
 */
@Composable
internal fun MagnitudeDial(
    valueText: String,
    onOpenMagnitudeEducation: () -> Unit,
    educationDescription: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 160.dp
) {
    val colors = MaterialTheme.colorScheme
    val palette = listOf(
        Color(0xFF23788D), Color(0xFF2E91A2), Color(0xFF42B1B0),
        Color(0xFF6DBA9B), Color(0xFFABCB77), Color(0xFFD8CC59),
        Color(0xFFF1BE4D), Color(0xFFF5A24B), Color(0xFFF38B40),
        Color(0xFFEB6C42), Color(0xFFD6504C), Color(0xFF994D67)
    )

    Box(
        modifier = modifier
            .size(diameter)
            .semantics { contentDescription = educationDescription }
            .clickable(onClick = onOpenMagnitudeEducation)
            .testTag("detail_magnitude_info"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().testTag("detail_magnitude_dial")) {
            val d = min(size.width, size.height)
            val center = Offset(size.width / 2f, size.height / 2f)
            val outside = d * 0.49f
            val radius = d * 0.398f
            val ringWidth = d * 0.071f
            val bounds = Size(radius * 2f, radius * 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)

            // Halo y relieve abstracto, no mapa, intensidad ni superficie afectada.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.secondary.copy(alpha = 0.15f),
                        colors.surface.copy(alpha = 0.42f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = outside
                ),
                radius = outside,
                center = center
            )
            drawCircle(
                color = colors.secondary.copy(alpha = 0.21f),
                radius = outside * 0.99f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = colors.outlineVariant.copy(alpha = 0.7f),
                radius = radius,
                center = center,
                style = Stroke(width = ringWidth)
            )

            // Arco segmentado constante: deliberadamente no funciona como porcentaje.
            val start = 132f
            val totalDegrees = 276f
            val perSegment = totalDegrees / palette.size
            palette.forEachIndexed { index, color ->
                drawArc(
                    color = color,
                    startAngle = start + perSegment * index + 1.5f,
                    sweepAngle = perSegment - 3f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = bounds,
                    style = Stroke(width = ringWidth, cap = StrokeCap.Butt)
                )
            }
            drawCircle(
                color = colors.secondary.copy(alpha = 0.42f),
                radius = d * 0.315f,
                center = center,
                style = Stroke(width = 0.85.dp.toPx())
            )
            // Ornamento de onda. No se presenta como una señal sísmica medida.
            val wave = Path().apply {
                val y = center.y + d * 0.205f
                val x = center.x
                moveTo(x - d * 0.18f, y)
                lineTo(x - d * 0.085f, y)
                lineTo(x - d * 0.047f, y - d * 0.052f)
                lineTo(x - d * 0.004f, y + d * 0.067f)
                lineTo(x + d * 0.039f, y - d * 0.036f)
                lineTo(x + d * 0.072f, y)
                lineTo(x + d * 0.18f, y)
            }
            drawPath(
                path = wave,
                color = colors.secondary,
                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.testTag("detail_magnitude_dial_text")
        ) {
            Text(
                text = stringResource(R.string.detail_pilot_dial_label),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                color = colors.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = valueText,
                modifier = Modifier.testTag("detail_magnitude_primary"),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = if (valueText.length > 6) 27.sp else 35.sp,
                    lineHeight = 42.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = colors.onSurface
            )
        }
    }
}

/** Líneas de relieve decorativas, explícitamente NO derivadas de cartografía tectónica. */
@Composable
internal fun GeologicalHeaderContours(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        repeat(19) { i ->
            val y = h * (0.055f + i * 0.049f)
            val shift = i % 5
            val path = Path().apply {
                moveTo(-w * 0.06f, y)
                cubicTo(
                    w * 0.23f, y - h * (0.18f + shift * 0.012f),
                    w * 0.40f, y + h * 0.23f,
                    w * 0.62f, y + h * 0.06f
                )
                cubicTo(
                    w * 0.82f, y - h * 0.17f,
                    w * 0.97f, y - h * 0.10f,
                    w * 1.08f, y + h * 0.06f
                )
            }
            val highlight = i % 5 == 0
            drawPath(
                path,
                color = (if (highlight) colors.secondary else colors.tertiary)
                    .copy(alpha = if (highlight) 0.26f else 0.13f),
                style = Stroke(width = (if (highlight) 1.5f else 0.8f).dp.toPx())
            )
        }
    }
}
