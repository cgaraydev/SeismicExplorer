package com.cgaraydev.seismicexplorer.presentation.learn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cgaraydev.seismicexplorer.R

/** Ilustración conceptual, nunca una secuencia real de revisiones USGS. */
@Composable
internal fun LearnRevisionDiagram() {
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.learn_revision_diagram_accessibility)
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("learn_revision_diagram"),
        color = colors.surfaceContainerLow,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, colors.outlineVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.learn_revision_diagram_heading),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )
            Canvas(
                Modifier.fillMaxWidth().height(92.dp)
                    .clearAndSetSemantics { contentDescription = description }
            ) {
                val mid = size.height * .5f
                val first = Offset(size.width * .13f, mid)
                val second = Offset(size.width * .50f, mid)
                val third = Offset(size.width * .87f, mid)
                drawLine(colors.onSurfaceVariant, first, third, strokeWidth = 2.dp.toPx())
                listOf(first, second, third).forEachIndexed { index, point ->
                    val fill = if (index == 2) colors.secondary else colors.primary
                    drawCircle(fill, radius = 8.dp.toPx(), center = point)
                    drawCircle(colors.surfaceContainerLow, radius = 3.dp.toPx(), center = point)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    R.string.learn_revision_diagram_initial,
                    R.string.learn_revision_diagram_data,
                    R.string.learn_revision_diagram_review
                ).forEach { title ->
                    Text(
                        text = stringResource(title),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Text(
                text = stringResource(R.string.learn_revision_diagram_caveat),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}
