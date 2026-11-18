package com.cgaraydev.seismicexplorer.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cgaraydev.seismicexplorer.R
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus
import com.cgaraydev.seismicexplorer.presentation.common.AtlasWaveformIcon
import com.cgaraydev.seismicexplorer.presentation.common.FeedCacheStatusText
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Portada del Observatorio. Las métricas proceden exclusivamente del estado observable. */
@Composable
fun ObservatoryDashboard(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenEarthquake: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val statistics = remember(state.earthquakes, state.timeWindow, state.periodEndAt) {
        calculateObservatoryStatistics(
            earthquakes = state.earthquakes,
            window = state.timeWindow,
            endAt = state.periodEndAt ?: Instant.now()
        )
    }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val date = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            .withLocale(locale)
            .format(LocalDate.now())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("observatory_dashboard"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "masthead") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AtlasWaveformIcon(modifier = Modifier.size(30.dp, 24.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.testTag("observatory_settings")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_observatory_settings),
                            contentDescription = stringResource(R.string.observatory_settings),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.observatory_title),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item(key = "window") {
            ObservatoryPeriodSelector(state.timeWindow) {
                onAction(HomeAction.SelectTimeWindow(it))
            }
        }
        item(key = "sync") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.observatory_data_scope),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { onAction(HomeAction.Refresh) },
                        enabled = state.refreshState != HomeRefreshState.Refreshing,
                        modifier = Modifier.testTag("observatory_refresh")
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_atlas_refresh),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(stringResource(R.string.home_refresh), Modifier.padding(start = 4.dp))
                    }
                }
                if (state.refreshState == HomeRefreshState.Refreshing) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                if (state.feedCacheStatus != FeedCacheStatus.Unknown) {
                    FeedCacheStatusText(state.feedCacheStatus)
                }
                if (state.localReadFailed) {
                    Text(stringResource(R.string.home_local_error), color = MaterialTheme.colorScheme.error)
                }
                if (state.refreshState is HomeRefreshState.Failed) {
                    Text(
                        text = stringResource(R.string.observatory_sync_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        item(key = "metrics") {
            ObservatoryMetricPanel(
                statistics = statistics,
                state = state,
                onOpenCatalog = onOpenCatalog,
                onOpenEarthquake = onOpenEarthquake
            )
        }
        item(key = "histogram") {
            ObservatoryHourlyChart(statistics, state.timeWindow)
        }
        item(key = "extra_metrics") {
            ObservatorySecondaryMetrics(statistics)
        }
        item(key = "footnote") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.observatory_data_caveat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.home_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text(stringResource(R.string.observatory_settings)) },
            text = { Text(stringResource(R.string.observatory_settings_description)) },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text(stringResource(R.string.observatory_close))
                }
            }
        )
    }
}

@Composable
private fun ObservatoryPeriodSelector(
    selected: TimeWindow,
    onSelect: (TimeWindow) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val period = when (selected) {
        TimeWindow.LAST_DAY -> R.string.observatory_window_day
        TimeWindow.LAST_WEEK -> R.string.observatory_window_week
        TimeWindow.LAST_MONTH -> R.string.observatory_window_month
    }
    Box {
        Surface(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { expanded = true }
                .testTag("observatory_window"),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_observatory_globe),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(period),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Icon(
                    painter = painterResource(R.drawable.ic_atlas_chevron_down),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TimeWindow.entries.forEach { window ->
                val resource = when (window) {
                    TimeWindow.LAST_DAY -> R.string.home_period_day
                    TimeWindow.LAST_WEEK -> R.string.home_period_week
                    TimeWindow.LAST_MONTH -> R.string.home_period_month
                }
                DropdownMenuItem(
                    text = { Text(stringResource(resource)) },
                    onClick = {
                        expanded = false
                        onSelect(window)
                    }
                )
            }
        }
    }
}

@Composable
private fun ObservatoryMetricPanel(
    statistics: ObservatoryStatistics,
    state: HomeUiState,
    onOpenCatalog: () -> Unit,
    onOpenEarthquake: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.4f
    val primarySurface = if (dark) Color(0xFF252C33) else Color(0xFFF7F9FA)
    val secondarySurface = if (dark) Color(0xFF30363D) else Color(0xFFDDE5EB)
    val mainInk = if (dark) Color(0xFFF4F6F7) else Color(0xFF18242D)
    val secondaryInk = if (dark) Color(0xFFBBC7CF) else Color(0xFF42545F)
    val accentInk = if (dark) Color(0xFFE8B08B) else Color(0xFF81462D)
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            onClick = onOpenCatalog,
            modifier = Modifier.weight(1f).height(184.dp)
                .testTag("observatory_events_card"),
            colors = CardDefaults.cardColors(containerColor = primarySurface),
            border = BorderStroke(1.dp, colors.outlineVariant),
            shape = shape
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = if (state.isReadingLocal && state.earthquakes.isEmpty()) "…"
                        else statistics.eventCount.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = when {
                        statistics.eventCount >= 100_000 -> 26.sp
                        statistics.eventCount >= 10_000 -> 32.sp
                        statistics.eventCount >= 1_000 -> 38.sp
                        else -> 48.sp
                    },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = mainInk,
                    modifier = Modifier.testTag("observatory_events_count")
                )
                Text(
                    stringResource(R.string.observatory_registered_events),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = mainInk,
                    maxLines = 2
                )
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.observatory_view_events),
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryInk,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        painterResource(R.drawable.ic_atlas_events),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = secondaryInk
                    )
                }
            }
        }

        Card(
            onClick = {
                statistics.largestEvent?.let { onOpenEarthquake(it.id) }
            },
            enabled = statistics.largestEvent != null,
            modifier = Modifier.weight(1f).height(184.dp)
                .testTag("observatory_max_card"),
            colors = CardDefaults.cardColors(
                containerColor = secondarySurface,
                disabledContainerColor = secondarySurface
            ),
            border = BorderStroke(1.dp, colors.outlineVariant),
            shape = shape
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = statistics.largestEvent?.magnitude?.let {
                        stringResource(R.string.home_event_magnitude, it)
                    } ?: stringResource(R.string.home_event_magnitude_short_unknown),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentInk,
                    maxLines = 1,
                    modifier = Modifier.testTag("observatory_max_value")
                )
                Text(
                    stringResource(R.string.observatory_maximum),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = mainInk,
                    maxLines = 2
                )
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(
                            if (statistics.largestEvent == null) R.string.observatory_no_max_event
                            else R.string.observatory_view_detail
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryInk,
                        modifier = Modifier.weight(1f)
                    )
                    if (statistics.largestEvent != null) {
                        Icon(
                            painterResource(R.drawable.ic_observatory_arrow_right),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = secondaryInk
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ObservatoryHourlyChart(
    statistics: ObservatoryStatistics,
    selectedWindow: TimeWindow
) {
    val colors = MaterialTheme.colorScheme
    val histogram = statistics.bucketCounts
    val largest = histogram.maxOrNull()?.coerceAtLeast(1) ?: 1
    val description = stringResource(R.string.observatory_chart_accessibility, histogram.sum())
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("observatory_chart"),
        color = colors.surfaceContainerLow,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(
                    if (statistics.hourly) R.string.observatory_events_per_hour
                    else R.string.observatory_events_per_day
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Canvas(
                modifier = Modifier.fillMaxWidth().height(138.dp)
                    .clearAndSetSemantics { contentDescription = description }
            ) {
                val bottom = size.height - 2.dp.toPx()
                val step = size.width / histogram.size
                repeat(4) { line ->
                    val y = bottom * line / 3f
                    drawLine(
                        color = colors.outlineVariant.copy(alpha = 0.65f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                histogram.forEachIndexed { index, value ->
                    if (value == 0) return@forEachIndexed
                    val barHeight = bottom * (value.toFloat() / largest)
                    val barWidth = step * if (histogram.size == 30) 0.64f else 0.73f
                    drawRect(
                        color = if (colors.background.luminance() < 0.4f)
                            Color(0xFF7498AE) else Color(0xFF426680),
                        topLeft = Offset(index * step + (step - barWidth) / 2f, bottom - barHeight),
                        size = Size(barWidth, barHeight)
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val labels = when (selectedWindow) {
                    TimeWindow.LAST_DAY -> listOf("-24 h", "-18 h", "-12 h", "-6 h", "Ahora")
                    TimeWindow.LAST_WEEK -> listOf("-7 d", "-5 d", "-3 d", "-1 d", "Hoy")
                    TimeWindow.LAST_MONTH -> listOf("-30 d", "-23 d", "-15 d", "-7 d", "Hoy")
                }
                labels.forEach {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }
            if (statistics.eventCount == 0) {
                Text(
                    text = stringResource(R.string.observatory_no_events),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ObservatorySecondaryMetrics(statistics: ObservatoryStatistics) {
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier = Modifier.testTag("observatory_extra_metrics"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(R.string.observatory_other_statistics),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ObservatoryMinorMetric(
                modifier = Modifier.weight(1f),
                value = statistics.magnitudeAtLeast45.toString(),
                label = stringResource(R.string.observatory_above_45)
            )
            ObservatoryMinorMetric(
                modifier = Modifier.weight(1f),
                value = statistics.magnitudeAtLeast25.toString(),
                label = stringResource(R.string.observatory_above_25)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ObservatoryMinorMetric(
                modifier = Modifier.weight(1f),
                value = statistics.averageDepthKilometers?.let {
                    String.format(locale, "%.0f km", it)
                } ?: stringResource(R.string.home_event_magnitude_short_unknown),
                label = stringResource(R.string.observatory_average_depth)
            )
            ObservatoryMinorMetric(
                modifier = Modifier.weight(1f),
                value = statistics.maxDepthKilometers?.let {
                    String.format(locale, "%.0f km", it)
                } ?: stringResource(R.string.home_event_magnitude_short_unknown),
                label = stringResource(R.string.observatory_max_depth)
            )
        }
    }
}

@Composable
private fun ObservatoryMinorMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.defaultMinSize(minHeight = 96.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                maxLines = 1)
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}
