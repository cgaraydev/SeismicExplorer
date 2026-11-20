package com.cgaraydev.seismicexplorer.presentation.detail

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cgaraydev.seismicexplorer.R

/** Secciones visuales del expediente. No disparan consultas de red al seleccionarse. */
internal enum class EventDetailTab(
    @StringRes val labelRes: Int,
    val testTag: String
) {
    RESUMEN(R.string.detail_tab_summary, "detail_tab_summary"),
    IMPACTO(R.string.detail_tab_impact, "detail_tab_impact"),
    CIENCIA(R.string.detail_tab_science, "detail_tab_science"),
    DATOS(R.string.detail_tab_data, "detail_tab_data")
}

/** Selector propio del evento; no sustituye la navegación global de la app. */
@Composable
internal fun EarthquakeEventTabs(
    selected: EventDetailTab,
    onSelect: (EventDetailTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surfaceContainerLow)
            .testTag("detail_event_tabs"),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        EventDetailTab.entries.forEach { tab ->
            val active = tab == selected
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Tab(
                    selected = active,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.fillMaxWidth().testTag(tab.testTag),
                    selectedContentColor = scheme.secondary,
                    unselectedContentColor = scheme.onSurfaceVariant,
                    text = {
                        Text(
                            stringResource(tab.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.75f)
                        .height(3.dp)
                        .padding(bottom = 1.dp)
                        .background(
                            if (active) scheme.secondary else scheme.surfaceContainerLow,
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}
