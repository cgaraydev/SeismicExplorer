package com.cgaraydev.seismicexplorer.presentation.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.cgaraydev.seismicexplorer.R
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun FeedCacheStatusText(
    status: FeedCacheStatus,
    modifier: Modifier = Modifier
) {
    val text = when (status) {
        FeedCacheStatus.Unknown -> return

        FeedCacheStatus.Unverified -> {
            stringResource(R.string.feed_status_unverified)
        }

        is FeedCacheStatus.Synced -> {
            val locale = LocalConfiguration.current.locales[0]

            val formattedDate = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(ZoneId.systemDefault())
                .format(
                    Instant.ofEpochMilli(
                        status.lastSuccessEpochMillis
                    )
                )

            stringResource(
                if (status.isFresh) {
                    R.string.feed_status_fresh
                } else {
                    R.string.feed_status_stale
                },
                formattedDate
            )
        }
    }

    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}