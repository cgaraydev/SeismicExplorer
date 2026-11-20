package com.cgaraydev.seismicexplorer.presentation.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cgaraydev.seismicexplorer.R

/** This grid navigates only. It never starts a download or infers product availability. */
@Composable
internal fun ScientificProductGrid(
    shakeMapDescription: String,
    onOpenShakeMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var plannedProduct by rememberSaveable { mutableStateOf<String?>(null) }
    val products = listOf(
        Triple("ShakeMap", R.drawable.ic_detail_shakemap, R.string.detail_ocean_shakemap),
        Triple("DYFI", R.drawable.ic_detail_dyfi, R.string.detail_ocean_dyfi),
        Triple("PAGER", R.drawable.ic_detail_pager, R.string.detail_ocean_pager),
        Triple(stringResource(R.string.detail_ocean_focal_title), R.drawable.ic_detail_focal,
            R.string.detail_ocean_focal)
    )
    BoxWithConstraints(modifier.fillMaxWidth().testTag("detail_product_grid")) {
        val columns = if (maxWidth < 300.dp || LocalConfiguration.current.fontScale > 1.3f) 1 else 2
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            products.chunked(columns).forEach { row ->
                Row(modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (title, icon, description) ->
                        val shakeMap = icon == R.drawable.ic_detail_shakemap
                        Surface(
                            onClick = { if (shakeMap) onOpenShakeMap() else plannedProduct = title },
                            modifier = Modifier.weight(1f).fillMaxHeight().testTag(when (icon) {
                                R.drawable.ic_detail_shakemap -> "detail_jump_intensity"
                                R.drawable.ic_detail_dyfi -> "detail_product_dyfi"
                                R.drawable.ic_detail_pager -> "detail_product_pager"
                                else -> "detail_product_focal"
                            }),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(Modifier.heightIn(min = 168.dp).padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Icon(painterResource(icon), null, Modifier.size(30.dp),
                                    tint = MaterialTheme.colorScheme.secondary)
                                Text(title, style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (shakeMap) shakeMapDescription else
                                    stringResource(R.string.detail_ocean_viewer_planned),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
    plannedProduct?.let { title ->
        AlertDialog(
            modifier = Modifier.testTag("detail_product_planned_dialog"),
            onDismissRequest = { plannedProduct = null },
            title = { Text(title) },
            text = { Text(stringResource(R.string.detail_ocean_planned_explanation)) },
            confirmButton = {
                TextButton(onClick = { plannedProduct = null }) {
                    Text(stringResource(R.string.detail_visual_close))
                }
            }
        )
    }
}
