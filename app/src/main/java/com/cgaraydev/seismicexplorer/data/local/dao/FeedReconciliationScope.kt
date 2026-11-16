package com.cgaraydev.seismicexplorer.data.local.dao

/** Límites del snapshot USGS confirmado. El extremo final es exclusivo. */
data class FeedReconciliationScope(
    val startEpochMillis: Long,
    val endEpochMillis: Long
)
