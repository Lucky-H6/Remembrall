package com.memoryball.app.ui.map

import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.memoryball.app.data.model.Place
import com.memoryball.app.data.repo.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapState(
    val name: String = "",
    val lat: Double = 39.9042, // Beijing default
    val lng: Double = 116.4074,
    val latText: String = "39.9042",
    val lngText: String = "116.4074",
    val radiusMeters: Float = 50f,
    val focusCount: Int = 0,
    val saved: Boolean = false,
    val error: String? = null
)

class MapViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ReminderRepository(app)
    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    private var editingId: Long = -1L
    private var originalCreatedAt: Long = 0L

    fun load(id: Long) {
        if (id == -1L) return
        editingId = id
        viewModelScope.launch {
            val p = repo.getPlaceById(id) ?: return@launch
            originalCreatedAt = p.createdAt
            _state.value = MapState(
                name = p.name,
                lat = p.latitude,
                lng = p.longitude,
                latText = p.latitude.toString(),
                lngText = p.longitude.toString(),
                radiusMeters = p.radiusMeters,
                focusCount = _state.value.focusCount + 1
            )
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setRadius(v: Float) = _state.update { it.copy(radiusMeters = v) }

    fun setLocation(lat: Double, lng: Double) = _state.update {
        it.copy(lat = lat, lng = lng, latText = lat.toString(), lngText = lng.toString())
    }

    fun setLatText(v: String) = _state.update {
        it.copy(latText = v, lat = v.toDoubleOrNull() ?: it.lat)
    }

    fun setLngText(v: String) = _state.update {
        it.copy(lngText = v, lng = v.toDoubleOrNull() ?: it.lng)
    }

    /** Fetch the device's current location and re-center the map. */
    fun requestMyLocation(context: Context) {
        try {
            AMapLocationClient.updatePrivacyShow(context, true, true)
            AMapLocationClient.updatePrivacyAgree(context, true)
            val client = AMapLocationClient(context)
            val option = AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                isOnceLocationLatest = true
            }
            client.setLocationOption(option)
            client.setLocationListener { loc ->
                if (loc != null && loc.errorCode == 0) {
                    setLocation(loc.latitude, loc.longitude)
                    _state.update { it.copy(focusCount = it.focusCount + 1) }
                }
                client.stopLocation()
                client.onDestroy()
            }
            client.startLocation()
        } catch (e: Exception) {
            // Fallback to system GPS provider
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                @Suppress("MissingPermission")
                val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    setLocation(loc.latitude, loc.longitude)
                    _state.update { it.copy(focusCount = it.focusCount + 1) }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(error = "请填写地点名称") }
            return
        }
        val place = Place(
            id = if (editingId == -1L) 0 else editingId,
            name = s.name.trim(),
            latitude = s.lat,
            longitude = s.lng,
            radiusMeters = s.radiusMeters,
            createdAt = if (editingId == -1L) System.currentTimeMillis() else originalCreatedAt
        )
        viewModelScope.launch {
            repo.savePlace(place)
            _state.update { it.copy(saved = true) }
        }
    }
}
