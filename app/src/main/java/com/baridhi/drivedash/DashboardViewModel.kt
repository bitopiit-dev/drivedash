package com.baridhi.drivedash

import android.app.Application
import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class GeoPoint(val lat: Double, val lng: Double)
data class Destination(val name: String, val point: GeoPoint)

data class DashState(
    val speedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val avgSpeedKmh: Float = 0f,
    val tripDistanceM: Float = 0f,
    val tripTimeSec: Long = 0L,
    val headingDeg: Float = 0f,
    val altitudeM: Double = 0.0,
    val accuracyM: Float = 0f,
    val hasFix: Boolean = false,
    val current: GeoPoint? = null,
    val destination: Destination? = null,
    val remainingM: Float? = null,
    val startDistanceM: Float? = null,
    val bearingToDestDeg: Float? = null,
    val etaSec: Long? = null,
    val useMph: Boolean = false,
    val speedLimitKmh: Float = 80f,
    val alertSound: Boolean = true,
    val isTracking: Boolean = false,
    val searching: Boolean = false,
    val message: String? = null
) {
    val isOverSpeed: Boolean get() = isTracking && speedKmh > speedLimitKmh
    val progress: Float
        get() {
            val start = startDistanceM ?: return 0f
            val left = remainingM ?: return 0f
            if (start <= 0f) return 1f
            return (1f - left / start).coerceIn(0f, 1f)
        }
}

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val tracker = LocationTracker(app)
    private val _state = MutableStateFlow(DashState())
    val state: StateFlow<DashState> = _state.asStateFlow()

    private var locationJob: Job? = null
    private var timerJob: Job? = null
    private var lastLocation: Location? = null
    private var smoothedKmh = 0f
    private var movingSec = 0.0
    private var arrivedNotified = false

    // ---------- Tracking ----------

    fun startTracking() {
        if (locationJob?.isActive == true) return
        _state.update { it.copy(isTracking = true) }
        locationJob = viewModelScope.launch {
            tracker.updates()
                .catch { e -> _state.update { it.copy(message = "GPS unavailable: ${e.message}") } }
                .collect { onLocation(it) }
        }
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _state.update { it.copy(tripTimeSec = it.tripTimeSec + 1) }
            }
        }
    }

    fun pauseTracking() {
        locationJob?.cancel(); locationJob = null
        timerJob?.cancel(); timerJob = null
        lastLocation = null
        smoothedKmh = 0f
        _state.update { it.copy(isTracking = false, speedKmh = 0f) }
    }

    fun resetTrip() {
        lastLocation = null
        movingSec = 0.0
        _state.update {
            it.copy(maxSpeedKmh = 0f, avgSpeedKmh = 0f, tripDistanceM = 0f, tripTimeSec = 0L)
        }
    }

    private fun onLocation(loc: Location) {
        val prev = lastLocation
        var rawKmh = if (loc.hasSpeed()) loc.speed * 3.6f else 0f
        var addedM = 0f

        if (prev != null) {
            val dt = (loc.elapsedRealtimeNanos - prev.elapsedRealtimeNanos) / 1_000_000_000.0
            if (dt > 0) {
                val d = prev.distanceTo(loc)
                if (!loc.hasSpeed()) rawKmh = (d / dt * 3.6).toFloat()
                // Ignore GPS jitter: only count distance when the fix is good and we're moving
                if (loc.accuracy <= 25f && rawKmh >= 3f) {
                    addedM = d
                    movingSec += dt
                }
            }
        }
        lastLocation = loc

        if (rawKmh < 2f) rawKmh = 0f
        smoothedKmh = if (smoothedKmh == 0f) rawKmh else 0.6f * rawKmh + 0.4f * smoothedKmh
        if (rawKmh == 0f) smoothedKmh = 0f

        _state.update { s ->
            val trip = s.tripDistanceM + addedM
            val avg = if (movingSec > 5) (trip / movingSec * 3.6).toFloat() else s.avgSpeedKmh
            val heading = if (loc.hasBearing() && smoothedKmh > 5f) loc.bearing else s.headingDeg
            withDestinationInfo(
                s.copy(
                    speedKmh = smoothedKmh,
                    maxSpeedKmh = maxOf(s.maxSpeedKmh, smoothedKmh),
                    avgSpeedKmh = avg,
                    tripDistanceM = trip,
                    headingDeg = heading,
                    altitudeM = if (loc.hasAltitude()) loc.altitude else s.altitudeM,
                    accuracyM = loc.accuracy,
                    hasFix = true,
                    current = GeoPoint(loc.latitude, loc.longitude)
                )
            )
        }
    }

    /** Recomputes remaining distance, direction and ETA to the destination. */
    private fun withDestinationInfo(s: DashState): DashState {
        val dest = s.destination ?: return s
        val cur = s.current ?: return s

        val result = FloatArray(2)
        Location.distanceBetween(cur.lat, cur.lng, dest.point.lat, dest.point.lng, result)
        val remaining = result[0]
        val bearing = (result[1] + 360f) % 360f

        val etaSpeedKmh = when {
            s.avgSpeedKmh > 5f -> s.avgSpeedKmh
            s.speedKmh > 5f -> s.speedKmh
            else -> null
        }
        val eta = etaSpeedKmh?.let { (remaining / (it / 3.6f)).toLong() }

        var msg = s.message
        if (remaining < 50f && !arrivedNotified) {
            arrivedNotified = true
            msg = "You've arrived at ${dest.name.substringBefore(',')}"
        }

        return s.copy(
            remainingM = remaining,
            startDistanceM = s.startDistanceM ?: remaining,
            bearingToDestDeg = bearing,
            etaSec = eta,
            message = msg
        )
    }

    // ---------- Destination ----------

    fun searchDestination(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(searching = true) }
            val dest = withContext(Dispatchers.IO) { parseCoordinates(q) ?: geocode(q) }
            if (dest == null) {
                _state.update {
                    it.copy(searching = false, message = "Couldn't find \"$q\". Try a fuller name or enter lat,lng.")
                }
            } else {
                arrivedNotified = false
                _state.update {
                    withDestinationInfo(
                        it.copy(
                            searching = false, destination = dest,
                            startDistanceM = null, remainingM = null, etaSec = null, bearingToDestDeg = null
                        )
                    )
                }
            }
        }
    }

    fun clearDestination() {
        _state.update {
            it.copy(destination = null, remainingM = null, startDistanceM = null, etaSec = null, bearingToDestDeg = null)
        }
    }

    private fun parseCoordinates(q: String): Destination? {
        val parts = q.split(",").map { it.trim() }
        if (parts.size != 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lng = parts[1].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        return Destination("%.5f, %.5f".format(Locale.US, lat, lng), GeoPoint(lat, lng))
    }

    private fun geocode(q: String): Destination? = try {
        if (!Geocoder.isPresent()) null else {
            @Suppress("DEPRECATION")
            val results = Geocoder(getApplication(), Locale.getDefault()).getFromLocationName(q, 1)
            results?.firstOrNull()?.let { a ->
                Destination(a.getAddressLine(0) ?: q, GeoPoint(a.latitude, a.longitude))
            }
        }
    } catch (e: Exception) {
        null
    }

    // ---------- Settings ----------

    fun setUseMph(mph: Boolean) = _state.update { it.copy(useMph = mph) }
    fun setSpeedLimitKmh(kmh: Float) = _state.update { it.copy(speedLimitKmh = kmh) }
    fun setAlertSound(on: Boolean) = _state.update { it.copy(alertSound = on) }
    fun clearMessage() = _state.update { it.copy(message = null) }
}
