package com.cgaraydev.seismicexplorer.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cgaraydev.seismicexplorer.R

/**
 * Cabecera editorial de ancho completo. El arte es decorativo, no un dato sísmico.
 * La imagen ocupa incluso la zona tras la barra del sistema; el contenido respeta
 * el inset superior y la altura puede crecer cuando aumenta el tamaño de fuente.
 */
@Composable
fun AtlasExploreHeader(modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val ink = if (dark) Color(0xFFF1F3EF) else Color(0xFF183344)
    val paper = if (dark) Color(0xFF14232D) else Color(0xFFF4EBDD)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 194.dp)
            .background(paper)
            .testTag("explore_header")
    ) {
        Image(
            painter = painterResource(R.drawable.atlas_mineral_explore),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    if (dark) {
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xF0101D27),
                                Color(0xDE18303A),
                                Color(0x62101D27)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFF9F0E4),
                                Color(0xEBF9F0E4),
                                Color(0x0CF9F0E4)
                            )
                        )
                    }
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .statusBarsPadding()
                .fillMaxWidth(0.87f)
                .padding(
                    start = 20.dp,
                    end = 12.dp,
                    top = 10.dp,
                    bottom = 18.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AtlasWaveformIcon(
                    modifier = Modifier.size(width = 32.dp, height = 22.dp),
                    color = if (dark) Color(0xFFF3AC90) else Color(0xFFA04F30)
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                maxLines = 3
            )
        }
    }
}

/** Icono decorativo: una onda estilizada, sin atribución a un registro real. */
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
