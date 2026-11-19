package com.cgaraydev.seismicexplorer.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cgaraydev.seismicexplorer.R

/** Roadmap de interfaz; no representa disponibilidad del catálogo de este evento. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailFutureFeatures() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { expanded = true }, modifier = Modifier
        .fillMaxWidth().testTag("detail_future_open")) {
        Text(stringResource(R.string.detail_visual_future_access))
    }
    if (expanded) {
        ModalBottomSheet(onDismissRequest = { expanded = false },
            modifier = Modifier.testTag("detail_future_sheet")) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(stringResource(R.string.detail_visual_future_title),
                    style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.detail_visual_future_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                val items = listOf(
                    Triple(R.drawable.ic_detail_wave, R.string.detail_visual_dyfi, R.string.detail_visual_dyfi_body),
                    Triple(R.drawable.ic_detail_layers, R.string.detail_visual_impact, R.string.detail_visual_impact_body),
                    Triple(R.drawable.ic_detail_region, R.string.detail_visual_tectonic, R.string.detail_visual_tectonic_body),
                    Triple(R.drawable.ic_detail_layers, R.string.detail_visual_rupture, R.string.detail_visual_rupture_body),
                    Triple(R.drawable.ic_detail_wave, R.string.detail_visual_stations, R.string.detail_visual_stations_body),
                    Triple(R.drawable.ic_detail_region, R.string.detail_visual_history, R.string.detail_visual_history_body)
                )
                items.forEach { (icon, title, body) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top) {
                        Icon(painterResource(icon), null, Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.secondary)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.detail_visual_planned),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary)
                            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                TextButton(onClick = { expanded = false }, modifier = Modifier.fillMaxWidth()
                    .testTag("detail_future_close")) {
                    Text(stringResource(R.string.detail_visual_close))
                }
            }
        }
    }
}
