package com.baridhi.drivedash.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baridhi.drivedash.DashState
import com.baridhi.drivedash.DashboardViewModel
import com.baridhi.drivedash.ui.theme.Dash
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

private fun hasLocationPermission(context: Context) = LOCATION_PERMISSIONS.any {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun DashboardScreen(vm: DashboardViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(hasLocationPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionGranted = result.values.any { it }
        if (permissionGranted) vm.startTracking()
    }

    LaunchedEffect(Unit) {
        if (permissionGranted) vm.startTracking() else permissionLauncher.launch(LOCATION_PERMISSIONS)
    }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    OverSpeedBeeper(active = state.isOverSpeed && state.alertSound)

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Dash.SkyTop, Dash.SkyBottom)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TopBar(state, onUnitChange = vm::setUseMph)

            if (!permissionGranted) {
                PermissionPanel { permissionLauncher.launch(LOCATION_PERMISSIONS) }
            }

            val mph = state.useMph
            Speedometer(
                speed = speedIn(state.speedKmh, mph),
                maxScale = if (mph) 140f else 220f,
                limit = speedIn(state.speedLimitKmh, mph),
                unit = speedUnit(mph),
                overSpeed = state.isOverSpeed,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            DestinationPanel(
                state = state,
                onSearch = vm::searchDestination,
                onClear = vm::clearDestination
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val (d, du) = formatDistance(state.tripDistanceM, mph)
                StatTile(Icons.Filled.Route, "Trip", d, du, Dash.Cyan)
                StatTile(Icons.Filled.Timer, "Time", formatDuration(state.tripTimeSec), "", Dash.Violet)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(Icons.Filled.Insights, "Average", "%.0f".format(speedIn(state.avgSpeedKmh, mph)), speedUnit(mph), Dash.Lime)
                StatTile(Icons.Filled.Bolt, "Top speed", "%.0f".format(speedIn(state.maxSpeedKmh, mph)), speedUnit(mph), Dash.Amber)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val alt = if (mph) state.altitudeM * 3.28084 else state.altitudeM
                StatTile(Icons.Filled.Terrain, "Altitude", "%.0f".format(alt), if (mph) "ft" else "m", Dash.Blue)
                CompassTile(state.headingDeg)
            }

            SpeedLimitPanel(
                limitKmh = state.speedLimitKmh,
                mph = mph,
                soundOn = state.alertSound,
                onLimitChange = vm::setSpeedLimitKmh,
                onSoundChange = vm::setAlertSound
            )

            Controls(
                tracking = state.isTracking,
                enabled = permissionGranted,
                onStart = vm::startTracking,
                onPause = vm::pauseTracking,
                onReset = vm::resetTrip
            )
            Spacer(Modifier.height(8.dp))
        }

        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)
        )
    }
}

// ---------------------------------------------------------------- building blocks

@Composable
private fun Panel(
    modifier: Modifier = Modifier,
    accent: Color = Dash.Cyan,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0x22FFFFFF), Color(0x08FFFFFF))))
            .border(1.dp, Brush.linearGradient(listOf(accent.copy(alpha = 0.55f), Dash.PanelEdge)), shape)
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun TopBar(state: DashState, onUnitChange: (Boolean) -> Unit) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) { now = LocalTime.now(); delay(1000) }
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "DriveDash",
                style = TextStyle(
                    brush = Brush.horizontalGradient(listOf(Dash.Cyan, Dash.Violet, Dash.Magenta)),
                    fontSize = 26.sp, fontWeight = FontWeight.Black
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(now.format(DateTimeFormatter.ofPattern("h:mm a")), color = Dash.TextDim, fontSize = 13.sp)
                Spacer(Modifier.width(10.dp))
                GpsChip(state)
            }
        }
        UnitToggle(mph = state.useMph, onChange = onUnitChange)
    }
}

@Composable
private fun GpsChip(state: DashState) {
    val good = state.hasFix && state.accuracyM <= 20f
    val color = when {
        !state.isTracking -> Dash.TextFaint
        good -> Dash.Lime
        state.hasFix -> Dash.Amber
        else -> Dash.Red
    }
    val label = when {
        !state.isTracking -> "GPS paused"
        state.hasFix -> "GPS ±${state.accuracyM.toInt()} m"
        else -> "Finding GPS…"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (state.hasFix) Icons.Filled.GpsFixed else Icons.Filled.GpsNotFixed,
            contentDescription = null, tint = color, modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun UnitToggle(mph: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x1AFFFFFF))
            .padding(3.dp)
    ) {
        listOf(false to "km/h", true to "mph").forEach { (value, label) ->
            val selected = mph == value
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) Brush.horizontalGradient(listOf(Dash.Cyan, Dash.Blue))
                        else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { onChange(value) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    label,
                    color = if (selected) Dash.SkyTop else Dash.TextDim,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PermissionPanel(onGrant: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), accent = Dash.Amber) {
        Text("Location access is off", color = Dash.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Speed and distance come from GPS. Allow location to start the dashboard.",
            color = Dash.TextDim, fontSize = 14.sp
        )
        Spacer(Modifier.height(12.dp))
        GradientButton("Allow location", Icons.Filled.GpsFixed, listOf(Dash.Amber, Dash.Magenta), onGrant)
    }
}

// ---------------------------------------------------------------- destination

@Composable
private fun DestinationPanel(
    state: DashState,
    onSearch: (String) -> Unit,
    onClear: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val submit = {
        focus.clearFocus()
        onSearch(query)
    }

    Panel(Modifier.fillMaxWidth(), accent = Dash.Magenta) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Where to? Place name or lat,lng", color = Dash.TextFaint) },
            leadingIcon = { Icon(Icons.Filled.Flag, contentDescription = null, tint = Dash.Magenta) },
            trailingIcon = {
                if (state.searching) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Dash.Magenta)
                } else {
                    IconButton(onClick = submit) {
                        Icon(Icons.Filled.Search, contentDescription = "Search destination", tint = Dash.Text)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Dash.Magenta,
                unfocusedBorderColor = Dash.PanelEdge,
                focusedTextColor = Dash.Text,
                unfocusedTextColor = Dash.Text,
                cursorColor = Dash.Magenta
            )
        )

        val dest = state.destination
        AnimatedVisibility(visible = dest != null) {
            if (dest == null) return@AnimatedVisibility
            Column {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dest.name,
                        color = Dash.TextDim, fontSize = 13.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { query = ""; onClear() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear destination", tint = Dash.TextDim)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        val remaining = state.remainingM
                        if (remaining == null) {
                            Text("Waiting for GPS…", color = Dash.TextDim, fontSize = 18.sp)
                        } else {
                            val (value, unit) = formatDistance(remaining, state.useMph)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    value,
                                    style = TextStyle(
                                        brush = Brush.horizontalGradient(listOf(Dash.Magenta, Dash.Amber)),
                                        fontSize = 46.sp, fontWeight = FontWeight.Black
                                    )
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(unit, color = Dash.TextDim, fontSize = 18.sp,
                                    modifier = Modifier.padding(bottom = 9.dp))
                            }
                            Text("to go, in a straight line", color = Dash.TextFaint, fontSize = 12.sp)
                        }
                    }
                    DirectionArrow(state)
                }

                Spacer(Modifier.height(14.dp))
                GradientProgress(state.progress)
                Spacer(Modifier.height(12.dp))

                Row {
                    MiniStat("Arrive in", state.etaSec?.let { formatEta(it) } ?: "—", Modifier.weight(1f))
                    MiniStat("Arrival", state.etaSec?.let { arrivalClock(it) } ?: "—", Modifier.weight(1f))
                    MiniStat("Done", "${(state.progress * 100).toInt()}%", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DirectionArrow(state: DashState) {
    val target = state.bearingToDestDeg ?: return
    // Arrow points to the destination relative to the direction the car is heading
    val relative = ((target - state.headingDeg) % 360f + 360f) % 360f
    val angle by animateFloatAsState(relative, tween(400), label = "dir")
    Box(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Dash.Magenta.copy(alpha = 0.35f), Color.Transparent))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Navigation,
            contentDescription = "Direction to destination",
            tint = Dash.Text,
            modifier = Modifier.size(36.dp).rotate(angle)
        )
    }
}

@Composable
private fun GradientProgress(progress: Float) {
    val animated by animateFloatAsState(progress, tween(600), label = "progress")
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0x1AFFFFFF))
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated.coerceAtLeast(0.02f))
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(Dash.Cyan, Dash.Violet, Dash.Magenta)))
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Dash.TextFaint, fontSize = 12.sp)
        Text(value, color = Dash.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------- stat tiles

@Composable
private fun RowScope.StatTile(icon: ImageVector, label: String, value: String, unit: String, accent: Color) {
    Panel(Modifier.weight(1f), accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(label, color = Dash.TextDim, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = Dash.Text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(4.dp))
                Text(unit, color = Dash.TextDim, fontSize = 13.sp, modifier = Modifier.padding(bottom = 5.dp))
            }
        }
    }
}

@Composable
private fun RowScope.CompassTile(heading: Float) {
    val angle by animateFloatAsState(heading, tween(500), label = "heading")
    Panel(Modifier.weight(1f), accent = Dash.Magenta) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .border(2.dp, Brush.sweepGradient(listOf(Dash.Cyan, Dash.Magenta, Dash.Cyan)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Navigation, contentDescription = null, tint = Dash.Magenta,
                    modifier = Modifier.size(26.dp).rotate(angle)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Heading", color = Dash.TextDim, fontSize = 13.sp)
                Text(
                    "${cardinal(heading)} ${heading.toInt()}°",
                    color = Dash.Text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// ---------------------------------------------------------------- limit + controls

@Composable
private fun SpeedLimitPanel(
    limitKmh: Float,
    mph: Boolean,
    soundOn: Boolean,
    onLimitChange: (Float) -> Unit,
    onSoundChange: (Boolean) -> Unit
) {
    val factor = if (mph) KMH_TO_MPH else 1f
    val range = if (mph) 15f..110f else 20f..180f
    Panel(Modifier.fillMaxWidth(), accent = Dash.Red) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Speed alert", color = Dash.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Warns above ${(limitKmh * factor).toInt()} ${speedUnit(mph)}",
                    color = Dash.TextDim, fontSize = 13.sp
                )
            }
            IconButton(onClick = { onSoundChange(!soundOn) }) {
                Icon(
                    if (soundOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                    contentDescription = if (soundOn) "Mute alert" else "Unmute alert",
                    tint = if (soundOn) Dash.Red else Dash.TextFaint
                )
            }
        }
        Slider(
            value = (limitKmh * factor).coerceIn(range),
            onValueChange = { onLimitChange(it / factor) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Dash.Red,
                inactiveTrackColor = Color(0x22FFFFFF)
            )
        )
    }
}

@Composable
private fun Controls(
    tracking: Boolean,
    enabled: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            if (tracking) {
                GradientButton("Pause trip", Icons.Filled.Pause, listOf(Dash.Amber, Dash.Magenta), onPause)
            } else {
                GradientButton(
                    "Start trip", Icons.Filled.PlayArrow, listOf(Dash.Cyan, Dash.Lime),
                    onClick = { if (enabled) onStart() }
                )
            }
        }
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .border(1.dp, Dash.PanelEdge, CircleShape)
                .background(Color(0x14FFFFFF))
                .clickable(onClick = onReset),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Reset trip", tint = Dash.Text)
        }
    }
}

@Composable
private fun GradientButton(text: String, icon: ImageVector, colors: List<Color>, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(colors))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Dash.SkyTop)
        Spacer(Modifier.width(8.dp))
        Text(text, color = Dash.SkyTop, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// ---------------------------------------------------------------- sound

@Composable
private fun OverSpeedBeeper(active: Boolean) {
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    LaunchedEffect(active) {
        while (active) {
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
            delay(4000)
        }
    }
}
