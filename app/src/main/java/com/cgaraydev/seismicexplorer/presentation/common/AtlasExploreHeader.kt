package com.cgaraydev.seismicexplorer.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cgaraydev.seismicexplorer.R

/** Arte original decorativo: no transmite información sísmica ni indica actividad en vivo. */
@Composable
fun AtlasExploreHeader(
    modifier: Modifier = Modifier
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val ink = if (dark) Color(0xFFF1F3EF) else Color(0xFF183344)
    val paper = if (dark) Color(0xFF14232D) else Color(0xFFF4EBDD)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(paper)
    ) {
        Image(
            painter = painterResource(R.drawable.atlas_mineral_explore),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )
        Box(
            Modifier.fillMaxSize().background(
                if (dark) Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xF0101D27), Color(0xD318303A), Color(0x5A101D27)
                    ), startX = 0f
                ) else Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFF9F0E4), Color(0xEBF9F0E4), Color(0x0CF9F0E4)
                    ), startX = 0f
                )
            )
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.85f)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AtlasWaveformIcon(
                    modifier = Modifier.size(width = 34.dp, height = 24.dp),
                    color = if (dark) Color(0xFFF3AC90) else Color(0xFFA04F30)
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelLarge,
                    color = ink,
                    maxLines = 1
                )
            }
            Text(
                text = stringResource(R.string.explore_header_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = ink
            )
            Text(
                text = stringResource(R.string.explore_header_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Icono original simple: una señal de onda estilizada, sin atribución a actividad real. */
@Composable
fun AtlasWaveformIcon(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary
) {
    Canvas(modifier) {
        val h = size.height
        val w = size.width
        val path = Path().apply {
            moveTo(0.02f * w, 0.53f * h)
            lineTo(0.24f * w, 0.53f * h)
            lineTo(0.35f * w, 0.22f * h)
            lineTo(0.45f * w, 0.82f * h)
            lineTo(0.56f * w, 0.07f * h)
            lineTo(0.67f * w, 0.7f * h)
            lineTo(0.77f * w, 0.53f * h)
            lineTo(0.98f * w, 0.53f * h)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
