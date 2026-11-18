package com.cgaraydev.seismicexplorer.presentation.map

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cgaraydev.seismicexplorer.R
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeDepthFilter
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.presentation.common.FeedCacheStatusText
import com.cgaraydev.seismicexplorer.presentation.map.location.CurrentLocationResult
import com.cgaraydev.seismicexplorer.presentation.map.location.DeviceLocationProvider
import com.cgaraydev.seismicexplorer.presentation.map.location.MapLocationStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng

@Composable
fun SeismicMapRoute(
    onBack: () -> Unit,
    onEarthquakeClick: (String) -> Unit,
    sharedFilters: MapFilterSelection? = null,
    onSharedTimeWindowChanged: (RecentWindow) -> Unit = {},
    onSharedMagnitudeChanged: (MapMagnitudeFilter) -> Unit = {},
    inTabNavigation: Boolean = false,
    viewModel: SeismicMapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedFilters by viewModel.filters.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    // El Observatorio gobierna el período y la magnitud al entrar en la pestaña.
    LaunchedEffect(sharedFilters?.timeWindow, sharedFilters?.magnitude) {
        sharedFilters?.let { filters ->
            viewModel.selectTimeWindow(filters.timeWindow)
            viewModel.selectMagnitude(filters.magnitude)
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Se comprueba la frescura del feed al abrir el mapa,
    // cambiar de período o reanudar la pantalla.
    LifecycleResumeEffect(selectedFilters.timeWindow) {
        val window = selectedFilters.timeWindow

        val syncJob = scope.launch {
            viewModel.ensureFeedFresh(window)
        }

        onPauseOrDispose {
            syncJob.cancel()
        }
    }

    val locationProvider = remember(context.applicationContext) {
        DeviceLocationProvider(context.applicationContext)
    }

    var cameraPosition by rememberSaveable {
        mutableStateOf(defaultSeismicCameraPosition())
    }

    // Evita recentrados automáticos al regresar
    // desde Detalle o al recrear la pantalla.
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

    var pendingLocationResolution by remember {
        mutableStateOf<PendingIntent?>(null)
    }

    var locationJob by remember {
        mutableStateOf<Job?>(null)
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
        pendingLocationResolution = null

        try {
            when (
                val result = locationProvider.getCurrentLocation()
            ) {
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

                is CurrentLocationResult.SettingsResolutionRequired -> {
                    pendingLocationResolution = result.resolution
                    locationStatus = MapLocationStatus.SETTINGS_REQUIRED
                }

                CurrentLocationResult.SettingsUnavailable -> {
                    locationStatus = MapLocationStatus.SETTINGS_UNAVAILABLE
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

    fun startLocationAttempt() {
        if (isLocating) return
        locationJob = scope.launch { locateDevice() }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startLocationAttempt()
        } else {
            locationStatus = MapLocationStatus.SETTINGS_REQUIRED
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startLocationAttempt()
            } else {
                locationStatus =
                    MapLocationStatus.PERMISSION_DENIED
            }
        }

    LaunchedEffect(Unit) {
        if (!autoLocationAttempted) {
            autoLocationAttempted = true

            if (hasLocationPermission()) {
                startLocationAttempt()
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        onPauseOrDispose {
            locationJob?.cancel()
        }
    }

    SeismicMapScreen(
        onBack = onBack,
        onEarthquakeClick = onEarthquakeClick,
        state = state,
        showBackButton = !inTabNavigation,
        onTimeWindowSelect = { window ->
            viewModel.selectTimeWindow(window)
            if (sharedFilters != null) onSharedTimeWindowChanged(window)
        },
        onMagnitudeSelect = { magnitude ->
            viewModel.selectMagnitude(magnitude)
            if (sharedFilters != null) onSharedMagnitudeChanged(magnitude)
        },
        onDepthFilterSelect = viewModel::selectDepthFilter,
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
                startLocationAttempt()
            } else {
                permissionLauncher.launch(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
        },
        onLocationSettingsClick = {
            val pending = pendingLocationResolution
            if (pending != null) {
                try {
                    settingsLauncher.launch(
                        IntentSenderRequest.Builder(pending).build()
                    )
                } catch (_: Exception) {
                    pendingLocationResolution = null
                    locationStatus = MapLocationStatus.SETTINGS_UNAVAILABLE
                }
            } else {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    )
                } catch (_: Exception) {
                    locationStatus = MapLocationStatus.SETTINGS_UNAVAILABLE
                }
            }
        },
        syncState = syncState,
        onSyncRetry = {
            scope.launch {
                viewModel.refreshFeed(
                    selectedFilters.timeWindow
                )
            }
        },
        onRefreshClick = {
            scope.launch {
                viewModel.refreshFeed(
                    selectedFilters.timeWindow
                )
            }
        },
        // Reintenta solo la consulta local, sin tocar USGS.
        onLocalRetry = viewModel::retryLocalObservation
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeismicMapScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    state: SeismicMapUiState = SeismicMapUiState(),
    onEarthquakeClick: (String) -> Unit = {},
    initialCamera: CameraPosition = defaultSeismicCameraPosition(),
    onCameraChanged: (CameraPosition) -> Unit = {},
    userPosition: LatLng? = null,
    centerUserRequestId: Int = 0,
    isLocating: Boolean = false,
    locationStatus: MapLocationStatus? = null,
    onLocateClick: () -> Unit = {},
    onLocationSettingsClick: () -> Unit = {},
    onTimeWindowSelect: (RecentWindow) -> Unit = {},
    onMagnitudeSelect: (MapMagnitudeFilter) -> Unit = {},
    syncState: MapSyncState = MapSyncState.Idle,
    onSyncRetry: () -> Unit = {},
    onDepthFilterSelect: (EarthquakeDepthFilter) -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onLocalRetry: () -> Unit = {},
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
    var resetCameraRequestId by remember {
        mutableIntStateOf(0)
    }

    var fitEventsRequestId by remember {
        mutableIntStateOf(0)
    }

    var showFilters by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = if (showBackButton) androidx.compose.material3.ScaffoldDefaults.contentWindowInsets
            else WindowInsets.statusBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.map_title))
                },
                navigationIcon = {
                    if (showBackButton) {
                        TextButton(onClick = onBack) {
                            Text(stringResource(R.string.detail_back))
                        }
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
                        verticalArrangement =
                            Arrangement.spacedBy(4.dp)
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
                                style =
                                    MaterialTheme.typography.bodySmall,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        when (val currentSync = syncState) {
                            MapSyncState.Idle -> {
                                TextButton(
                                    onClick = onRefreshClick,
                                    enabled = !state.isLoading
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.home_refresh
                                        )
                                    )
                                }
                            }

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

                                    TextButton(
                                        onClick = onRefreshClick
                                    ) {
                                        Text(
                                            stringResource(
                                                R.string.home_refresh
                                            )
                                        )
                                    }
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
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.map_local_error
                                )
                            )
                            TextButton(onClick = onLocalRetry) {
                                Text(
                                    text = stringResource(
                                        R.string.map_local_retry
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
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
                            contentDescription =
                                fitEventsDescription
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
                            contentDescription =
                                locationDescription
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

                        MapLocationStatus.SETTINGS_REQUIRED ->
                            R.string.map_location_settings_required

                        MapLocationStatus.SETTINGS_UNAVAILABLE ->
                            R.string.map_location_settings_unavailable

                        MapLocationStatus.UNAVAILABLE ->
                            R.string.map_location_temporarily_unavailable
                    }

                    Surface(
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = stringResource(messageRes))
                            if (
                                status == MapLocationStatus.SETTINGS_REQUIRED ||
                                status == MapLocationStatus.SETTINGS_UNAVAILABLE
                            ) {
                                TextButton(onClick = onLocationSettingsClick) {
                                    Text(
                                        stringResource(
                                            if (status == MapLocationStatus.SETTINGS_REQUIRED) {
                                                R.string.map_location_configure
                                            } else {
                                                R.string.map_location_open_settings
                                            }
                                        )
                                    )
                                }
                            }
                        }
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
            onDepthFilterSelect = onDepthFilterSelect,
            onDismiss = {
                showFilters = false
            }
        )
    }
}
