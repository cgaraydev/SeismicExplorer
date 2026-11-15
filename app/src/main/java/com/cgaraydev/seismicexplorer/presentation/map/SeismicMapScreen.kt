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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.res.stringResource
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


@Composable
fun SeismicMapRoute(
    onBack: () -> Unit,
    onEarthquakeClick: (String) -> Unit,
    viewModel: SeismicMapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                if (state.isLoading) {
                    Surface(
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = stringResource(
                                R.string.map_loading_events
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Surface(
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = stringResource(
                                R.string.map_event_count,
                                state.eventCount
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (state.hasError) {
                    Surface(
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
                FilledTonalButton(
                    enabled = state.eventBounds != null &&
                            !state.isLoading,
                    onClick = {
                        fitEventsRequestId++
                    }
                ) {
                    Text(
                        stringResource(R.string.map_fit_events)
                    )
                }


                FilledTonalButton(
                    onClick = onLocateClick,
                    enabled = !isLocating
                ) {
                    Text(
                        text = stringResource(
                            if (isLocating) {
                                R.string.map_locating
                            } else {
                                R.string.map_my_location
                            }
                        )
                    )
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
}