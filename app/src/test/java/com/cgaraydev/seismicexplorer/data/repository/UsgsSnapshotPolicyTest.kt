package com.cgaraydev.seismicexplorer.data.repository

import com.cgaraydev.seismicexplorer.data.mapper.toDomain
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeCollectionDto
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeFeatureDto
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeGeometryDto
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeMetadataDto
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakePropertiesDto
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsgsSnapshotPolicyTest {
    private val now = Instant.parse("2026-11-16T12:00:00Z").toEpochMilli()

    @Test fun validSnapshotEnablesBoundedReconciliation() {
        val response = response(listOf(earthquake("a")))
        val outcome = UsgsSnapshotPolicy.assess(
            response, response.toDomain(), RecentWindow.LAST_DAY, now
        ) as UsgsSnapshotDecision.Trusted
        assertEquals(now - RecentWindow.LAST_DAY.duration.toMillis(), outcome.scope.startEpochMillis)
        assertEquals(now, outcome.scope.endEpochMillis)
    }

    @Test fun emptyVerifiedSnapshotIsAllowed() {
        val response = response(emptyList())
        assertTrue(UsgsSnapshotPolicy.assess(
            response, emptyList(), RecentWindow.LAST_DAY, now
        ) is UsgsSnapshotDecision.Trusted)
    }

    @Test fun absentMetadataNeverAuthorizesDeletion() {
        val response = UsgsEarthquakeCollectionDto(listOf(earthquake("a")))
        assertEquals(UsgsSnapshotDecision.Unverifiable,
            UsgsSnapshotPolicy.assess(response, response.toDomain(), RecentWindow.LAST_DAY, now))
    }

    @Test fun inconsistentCountRejectsSnapshot() {
        val response = response(listOf(earthquake("a"))).copy(
            metadata = UsgsEarthquakeMetadataDto(generated = now, count = 2, status = 200)
        )
        assertEquals(UsgsSnapshotDecision.Invalid,
            UsgsSnapshotPolicy.assess(response, response.toDomain(), RecentWindow.LAST_DAY, now))
    }

    @Test fun invalidEarthquakeCannotBeMistakenForWithdrawal() {
        val invalid = earthquake("bad").copy(
            geometry = UsgsEarthquakeGeometryDto("Point", listOf(400.0, -33.0, 10.0))
        )
        val response = response(listOf(invalid))
        assertTrue(response.toDomain().isEmpty())
        assertEquals(UsgsSnapshotDecision.Invalid,
            UsgsSnapshotPolicy.assess(response, response.toDomain(), RecentWindow.LAST_DAY, now))
    }

    @Test fun reclassifiedEventCanDisappearFromLocalEarthquakeSet() {
        val reclassified = earthquake("old-earthquake").copy(
            properties = earthquake("old-earthquake").properties!!.copy(type = "quarry blast")
        )
        val response = response(listOf(reclassified))
        assertTrue(response.toDomain().isEmpty())
        assertTrue(UsgsSnapshotPolicy.assess(response, response.toDomain(), RecentWindow.LAST_DAY, now)
            is UsgsSnapshotDecision.Trusted)
    }

    @Test fun staleOrFutureSnapshotIsRejected() {
        val old = response(listOf(earthquake("a"))).copy(metadata =
            UsgsEarthquakeMetadataDto(generated = now - 2 * 60 * 60 * 1000L, count = 1, status = 200))
        assertEquals(UsgsSnapshotDecision.Invalid,
            UsgsSnapshotPolicy.assess(old, old.toDomain(), RecentWindow.LAST_DAY, now))
        val future = response(listOf(earthquake("a"))).copy(metadata =
            UsgsEarthquakeMetadataDto(generated = now + 10 * 60 * 1000L, count = 1, status = 200))
        assertEquals(UsgsSnapshotDecision.Invalid,
            UsgsSnapshotPolicy.assess(future, future.toDomain(), RecentWindow.LAST_DAY, now))
    }

    private fun response(features: List<UsgsEarthquakeFeatureDto>) =
        UsgsEarthquakeCollectionDto(
            features = features,
            metadata = UsgsEarthquakeMetadataDto(generated = now, count = features.size, status = 200)
        )

    private fun earthquake(id: String) = UsgsEarthquakeFeatureDto(
        id = id,
        properties = UsgsEarthquakePropertiesDto(
            mag = 4.2, time = now - 3_600_000, updated = now - 1_800_000, type = "earthquake"
        ),
        geometry = UsgsEarthquakeGeometryDto(type = "Point", coordinates = listOf(-71.6, -33.4, 10.0))
    )
}
