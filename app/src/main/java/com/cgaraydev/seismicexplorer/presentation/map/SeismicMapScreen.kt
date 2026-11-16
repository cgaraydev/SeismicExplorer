package com.cgaraydev.seismicexplorer.presentation.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cgaraydev.seismicexplorer.R
import com.cgaraydev.seismicexplorer.presentation.map.location.CurrentLocationResult
import com.cgaraydev.seismicexplorer.presentation.map.location.DeviceLocationProvider
import com.cgaraydev.seismicexplorer.presentation.map.location.MapLocationStatus
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import androidx.compose.foundation.layout.widthIn
import com.cgaraydev.seismicexplorer.presentation.common.FeedCacheStatusText

@Composable
fun SeismicMapRoute(
    onBack: () -> Unit,
    onEarthquakeClick: (String) -> Unit,
    viewModel: SeismicMapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val selectedFilters by viewModel.filters.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    var syncRetryRequestId by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(
        selectedFilters.timeWindow,
        syncRetryRequestId
    ) {
        viewModel.ensureFeedFresh(
            selectedFilters.timeWindow
        )
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val locationProvider = remember(context.applicationContext) {
        DeviceLocationProvider(context.applicationContext)
    }

    var cameraPosition by rememberSaveable {
        mutableStateOf(defaultSeismicCameraPosition())
    }

    // Impide recentrados automáticos al rotar o regresar
    // desde Detalle, conservando el estado de navegación.
    var autoLocationAttempted by rememberSaveable {
        mutableStateOf(false)
    }

    var userPosition by remember {
        mutableStateOf<LatLng?>(null)
    }

    var centerUserRequestId by rememberSaveable {
        mutableIntStateOf(0)
    }

    var isLocating by remember {
        mutableStateOf(false)
    }

    var locationStatus by remember {
        mutableStateOf<MapLocationStatus?>(null)
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun locateDevice() {
        if (isLocating) return

        isLocating = true
        locationStatus = null

        try {
            when (val result = locationProvider.getCurrentLocation()) {
                is CurrentLocationResult.Available -> {
                    userPosition = LatLng(
                        result.location.latitude,
                        result.location.longitude
                    )

                    centerUserRequestId++
                }

                CurrentLocationResult.PermissionRequired -> {
                    locationStatus =
                        MapLocationStatus.PERMISSION_DENIED
                }

                CurrentLocationResult.LocationDisabled -> {
                    locationStatus =
                        MapLocationStatus.LOCATION_DISABLED
                }

                CurrentLocationResult.Unavailable -> {
                    locationStatus =
                        MapLocationStatus.UNAVAILABLE
                }
            }
        } finally {
            isLocating = false
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                scope.launch {
                    locateDevice()
                }
            } else {
                locationStatus =
                    MapLocationStatus.PERMISSION_DENIED
            }
        }

    LaunchedEffect(Unit) {
        if (!autoLocationAttempted) {
            autoLocationAttempted = true

            if (hasLocationPermission()) {
                locateDevice()
            }
        }
    }

    SeismicMapScreen(
        onBack = onBack,
        onEarthquakeClick = onEarthquakeClick,
        state = state,
        onTimeWindowSelect = viewModel::selectTimeWindow,
        onMagnitudeSelect = viewModel::selectMagnitude,
        initialCamera = cameraPosition,
        onCameraChanged = { camera ->
            cameraPosition = camera
        },
        userPosition = userPosition,
        centerUserRequestId = centerUserRequestId,
        isLocating = isLocating,
        locationStatus = locationStatus,
        onLocateClick = {
            if (hasLocationPermission()) {
                scope.launch {
                    locateDevice()
                }
            } else {
                permissionLauncher.launch(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
        },
        syncState = syncState,
        onSyncRetry = {
            syncRetryRequestId++
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeismicMapScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    state: SeismicMapUiState = SeismicMapUiState(),
    onEarthquakeClick: (String) -> Unit = {},
    initialCamera: CameraPosition = defaultSeismicCameraPosition(),
    onCameraChanged: (CameraPosition) -> Unit = {},
    userPosition: LatLng? = null,
    centerUserRequestId: Int = 0,
    isLocating: Boolean = false,
    locationStatus: MapLocationStatus? = null,
    onLocateClick: () -> Unit = {},
    onTimeWindowSelect: (RecentWindow) -> Unit = {},
    onMagnitudeSelect: (MapMagnitudeFilter) -> Unit = {},
    syncState: MapSyncState = MapSyncState.Idle,
    onSyncRetry: () -> Unit = {},
    mapContent: @Composable (
        String,
        (String) -> Unit,
        Int,
        MapEventBounds?,
        Int
    ) -> Unit = { geoJson,
                  selectEarthquake,
                  resetRequestId,
                  bounds,
                  fitRequestId ->

        SeismicMapView(
            geoJson = geoJson,
            initialCamera = initialCamera,
            onEarthquakeClick = selectEarthquake,
            onCameraChanged = onCameraChanged,
            resetCameraRequestId = resetRequestId,
            eventBounds = bounds,
            fitEventsRequestId = fitRequestId,
            userPosition = userPosition,
            centerUserRequestId = centerUserRequestId
        )
    }
) {
    var resetCameraRequestId by remember { mutableIntStateOf(0) }
    var fitEventsRequestId by remember { mutableIntStateOf(0) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.map_title))
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.detail_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            showFilters = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                R.drawable.ic_map_filters
                            ),
                            contentDescription = stringResource(
                                R.string.map_filters
                            )
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            mapContent(
                state.geoJson,
                onEarthquakeClick,
                0,
                state.eventBounds,
                fitEventsRequestId
            )


            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = 300.dp),
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (state.isLoading) {
                            Text(
                                text = stringResource(
                                    R.string.map_loading_events
                                )
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    R.string.map_event_count,
                                    state.eventCount
                                )
                            )

                            Text(
                                text = mapFiltersSummary(state.filters),
                                style = MaterialTheme.typography.bodySmall,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        when (val currentSync = syncState) {
                            MapSyncState.Idle -> Unit

                            is MapSyncState.Syncing -> {
                                if (
                                    currentSync.window ==
                                    state.filters.timeWindow
                                ) {
                                    FeedCacheStatusText(
                                        currentSync.cacheStatus
                                    )

                                    Text(
                                        text = stringResource(
                                            R.string.map_syncing
                                        ),
                                        style =
                                            MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            is MapSyncState.Ready -> {
                                if (
                                    currentSync.window ==
                                    state.filters.timeWindow
                                ) {
                                    FeedCacheStatusText(
                                        currentSync.cacheStatus
                                    )
                                }
                            }

                            is MapSyncState.Failed -> {
                                if (
                                    currentSync.window ==
                                    state.filters.timeWindow
                                ) {
                                    FeedCacheStatusText(
                                        currentSync.cacheStatus
                                    )

                                    Text(
                                        text = stringResource(
                                            R.string.map_sync_failed
                                        ),
                                        style =
                                            MaterialTheme.typography.bodySmall
                                    )

                                    TextButton(
                                        onClick = onSyncRetry
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.map_sync_retry
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .fillMaxWidth()
                    )
                }

                if (state.hasError) {
                    Surface(
                        modifier = Modifier.widthIn(max = 300.dp),
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = stringResource(
                                R.string.map_local_error
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }



            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                val fitEventsDescription =
                    stringResource(R.string.map_fit_events)

                val locationDescription = stringResource(
                    if (isLocating) {
                        R.string.map_locating
                    } else {
                        R.string.map_my_location
                    }
                )

                FilledTonalIconButton(
                    onClick = {
                        fitEventsRequestId++
                    },
                    enabled = state.eventBounds != null &&
                            !state.isLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = fitEventsDescription
                        }
                ) {
                    Icon(
                        painter = painterResource(
                            R.drawable.ic_map_fit_events
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onLocateClick,
                    enabled = !isLocating,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = locationDescription
                        }
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(
                                R.drawable.ic_map_my_location
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }



                locationStatus?.let { status ->
                    val messageRes = when (status) {
                        MapLocationStatus.PERMISSION_DENIED ->
                            R.string.map_location_permission_denied

                        MapLocationStatus.LOCATION_DISABLED ->
                            R.string.map_location_disabled

                        MapLocationStatus.UNAVAILABLE ->
                            R.string.map_location_unavailable
                    }

                    Surface(
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = stringResource(messageRes),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }


            }


        }
    }

    if (showFilters) {
        SeismicMapFiltersSheet(
            filters = state.filters,
            onTimeWindowSelect = onTimeWindowSelect,
            onMagnitudeSelect = onMagnitudeSelect,
            onDismiss = {
                showFilters = false
            }
        )
    }
}