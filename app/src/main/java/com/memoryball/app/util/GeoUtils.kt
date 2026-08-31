package com.memoryball.app.util

import android.location.Location
import com.memoryball.app.data.model.Place

/** Distance helpers (WGS84, metres). */
object GeoUtils {
    /** Distance in metres between a point and a place centre. */
    fun distanceMeters(lat: Double, lng: Double, place: Place): Float {
        val out = FloatArray(1)
        Location.distanceBetween(lat, lng, place.latitude, place.longitude, out)
        return out[0]
    }

    fun isInside(lat: Double, lng: Double, place: Place): Boolean =
        distanceMeters(lat, lng, place) <= place.radiusMeters
}
