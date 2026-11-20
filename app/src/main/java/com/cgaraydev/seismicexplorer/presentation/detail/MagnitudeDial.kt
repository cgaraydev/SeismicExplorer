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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cgaraydev.seismicexplorer.R
import kotlin.math.min

/** Decorative continuous ring, never a risk scale or a percentage. */
@Composable
internal fun MagnitudeDial(
    valueText: String,
    onOpenMagnitudeEducation: () -> Unit,
    educationDescription: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 160.dp,
    hasMagnitude: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val fontScale = androidx.compose.ui.platform.LocalConfiguration.current.fontScale
    val palette = listOf(Color(0xFFE9584F), Color(0xFF733E67), Color(0xFF168DA5),
        Color(0xFF50BBA2), Color(0xFFE1CE58), Color(0xFFF3A34E), Color(0xFFE9584F))
    Box(
        modifier = modifier.size(diameter * fontScale.coerceIn(1f, 1.8f))
            .semantics { contentDescription = educationDescription }
            .clickable(onClick = onOpenMagnitudeEducation).testTag("detail_magnitude_info"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().testTag("detail_magnitude_dial")) {
            val d = min(size.width, size.height)
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(colors.surface.copy(alpha = 0.96f), radius = d * 0.43f, center = center)
            drawCircle(colors.secondary.copy(alpha = 0.16f), radius = d * 0.49f,
                center = center, style = Stroke(d * 0.012f))
            drawCircle(
                brush = Brush.sweepGradient(if (hasMagnitude) palette else
                    listOf(colors.outline, colors.outline), center),
                radius = d * 0.447f, center = center, style = Stroke(d * 0.047f)
            )
            drawCircle(colors.secondary.copy(alpha = 0.35f), radius = d * 0.413f,
                center = center, style = Stroke(0.7.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.testTag("detail_magnitude_dial_text")) {
            Text(stringResource(R.string.detail_pilot_dial_label),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp),
                color = colors.onSurfaceVariant)
            Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                    if (valueText.startsWith("M ")) {
                        withStyle(androidx.compose.ui.text.SpanStyle(fontSize = 19.sp,
                            color = colors.onSurface)) { append("M ") }
                        append(valueText.removePrefix("M "))
                    } else append(valueText)
                },
                modifier = Modifier.testTag("detail_magnitude_primary"),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = if (valueText.length > 8) 15.sp else 29.sp, lineHeight = 36.sp),
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                color = colors.secondary
            )
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_detail_wave),
                contentDescription = null, tint = colors.secondary,
                modifier = Modifier.size(27.dp)
            )
        }
    }
}
