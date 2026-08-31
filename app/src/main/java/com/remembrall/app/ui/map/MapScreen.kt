package com.remembrall.app.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.remembrall.app.BuildConfig

/**
 * Map-based place picker. The user drags the map; a fixed center pin marks the
 * chosen coordinate (the FakeLocation-style "move the map, not the marker" UX).
 * Falls back to manual coordinate entry if no AMap key is configured.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    placeId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    vm: MapViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(placeId) {
        vm.load(placeId)
        if (placeId == -1L) vm.requestMyLocation(context)
    }
    if (state.saved) {
        LaunchedEffect(Unit) { onSaved() }
    }

    Box(Modifier.fillMaxSize()) {
        if (BuildConfig.AMAP_KEY.isNotBlank()) {
            AMapPicker(
                initialLat = state.lat,
                initialLng = state.lng,
                focus = state.focusCount,
                onCenterChanged = { lat, lng -> vm.setLocation(lat, lng) },
                modifier = Modifier.fillMaxSize()
            )
            // Center pin: teardrop whose tip marks the exact screen center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(44.dp)
                        .offset(y = (-18).dp)
                )
            }
            // My-location FAB
            SmallFloatingActionButton(
                onClick = { vm.requestMyLocation(context) },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .offset(y = (-150).dp)
            ) {
                Icon(Icons.Default.MyLocation, "我的位置")
            }
        } else {
            // Fallback when no map key: manual coordinate entry.
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 72.dp)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "未配置高德地图 Key，使用手动输入坐标",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "配置 AMAP_KEY 后，即可在地图上拖动选点。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = state.latText,
                    onValueChange = { vm.setLatText(it) },
                    label = { Text("纬度 (latitude)") },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.lngText,
                    onValueChange = { vm.setLngText(it) },
                    label = { Text("经度 (longitude)") },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                FilledTonalButton(onClick = { vm.requestMyLocation(context) }) {
                    Icon(Icons.Default.MyLocation, null)
                    Spacer(Modifier.width(8.dp))
                    Text("使用当前位置")
                }
            }
        }

        // Back button floating on the map
        SmallFloatingActionButton(
            onClick = onBack,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
        }

        // Bottom editor card
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 36.dp, height = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(2.dp)
                        )
                )
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { vm.setName(it) },
                    label = { Text("地点名称") },
                    placeholder = { Text("如：家、公司、学校") },
                    leadingIcon = { Icon(Icons.Default.Radar, null) },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "触发半径",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${state.radiusMeters.toInt()} 米",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = state.radiusMeters,
                        onValueChange = { vm.setRadius(it) },
                        valueRange = 30f..1000f,
                        steps = 48
                    )
                }
                Text(
                    "坐标：%.6f, %.6f".format(state.lat, state.lng),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.error != null) {
                    Text(
                        state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = { vm.save() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("保存地点", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun AMapPicker(
    initialLat: Double,
    initialLng: Double,
    focus: Int,
    onCenterChanged: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val mapViewState = rememberMapViewWithLifecycle()
    var aMap by remember { mutableStateOf<AMap?>(null) }
    var lastFocus by remember { mutableStateOf(0) }

    AndroidView(factory = { mapViewState }, modifier = modifier.fillMaxSize()) { mapView ->
        if (aMap == null) {
            aMap = mapView.map
            lastFocus = focus
            aMap?.let { map ->
                map.uiSettings.isZoomControlsEnabled = false
                map.uiSettings.isMyLocationButtonEnabled = false
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(initialLat, initialLng), 17f)
                )
                map.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
                    override fun onCameraChange(position: CameraPosition?) {}
                    override fun onCameraChangeFinish(position: CameraPosition?) {
                        position?.target?.let { onCenterChanged(it.latitude, it.longitude) }
                    }
                })
            }
        } else if (focus != lastFocus) {
            lastFocus = focus
            aMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(initialLat, initialLng), 17f)
            )
        }
    }
}

@Composable
fun rememberMapViewWithLifecycle(): TextureMapView {
    val context = LocalContext.current
    // onCreate must run synchronously BEFORE the view is attached by AndroidView,
    // otherwise the renderer is not initialized and the first open shows a blank map.
    val mapView = remember {
        TextureMapView(context).apply { onCreate(null) }
    }
    DisposableEffect(mapView) {
        onDispose { mapView.onDestroy() }
    }
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onStart(owner: androidx.lifecycle.LifecycleOwner) { mapView.onResume() }
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) = mapView.onResume()
            override fun onPause(owner: androidx.lifecycle.LifecycleOwner) = mapView.onPause()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return mapView
}
