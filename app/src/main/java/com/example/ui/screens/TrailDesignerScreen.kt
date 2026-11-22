package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.maps.CorosRouteExporter
import com.example.data.model.ActivityType
import com.example.data.model.DistanceUnit
import com.example.data.model.GeoPointEle
import com.example.data.model.SummitFormatters
import com.example.data.model.UiState
import com.example.ui.components.OfflineTopoMapView
import com.example.ui.theme.WolfFrostBlue
import com.example.ui.viewmodel.SummitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailDesignerScreen(
    viewModel: SummitViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val trackingState by viewModel.trackingState.collectAsStateWithLifecycle()
    val savedTrails by viewModel.customTrailsState.collectAsStateWithLifecycle()

    val unit = (profileState as? UiState.Success)?.data?.unitEnum() ?: DistanceUnit.MILES
    val baseLat = trackingState.lastKnownLocation?.latitude ?: 40.0150
    val baseLon = trackingState.lastKnownLocation?.longitude ?: -105.2705

    var trailName by remember { mutableStateOf("Frostpine Ridge Loop") }
    var selectedDiscipline by remember { mutableStateOf(ActivityType.RUN) }
    var plottedPoints by remember { mutableStateOf<List<GeoPointEle>>(emptyList()) }

    val routeDistanceMiles = remember(plottedPoints) {
        CorosRouteExporter.calculateRouteDistanceMiles(plottedPoints)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trail Designer · COROS Export", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("trail_designer_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Text(
                    text = "Tap anywhere on the topo map below to drop GPS waypoints, save to your offline quest library, or export GPX 1.1 directly to your COROS watch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Interactive Tap-to-Plot Topo Map
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.5.dp, WolfFrostBlue.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                ) {
                    OfflineTopoMapView(
                        tileManager = viewModel.offlineMapTileManager,
                        centerLat = plottedPoints.lastOrNull()?.latitude ?: baseLat,
                        centerLon = plottedPoints.lastOrNull()?.longitude ?: baseLon,
                        routePoints = plottedPoints,
                        currentLocation = trackingState.lastKnownLocation,
                        offlineOnlyMode = trackingState.offlineOnlyMapMode,
                        initialZoom = 14,
                        interactive = true,
                        onMapTap = { tapped ->
                            plottedPoints = plottedPoints + tapped
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Quick plot controls + live stats
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "${plottedPoints.size} Waypoints Plotted",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = SummitFormatters.formatDistance(routeDistanceMiles, unit),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WolfFrostBlue
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        plottedPoints = CorosRouteExporter.generateStarterLoop(baseLat, baseLon, 3.0)
                                    },
                                    modifier = Modifier.testTag("seed_starter_loop_button")
                                ) {
                                    Icon(Icons.Filled.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("3 mi Loop")
                                }
                                IconButton(
                                    onClick = { if (plottedPoints.isNotEmpty()) plottedPoints = plottedPoints.dropLast(1) },
                                    enabled = plottedPoints.isNotEmpty()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo waypoint")
                                }
                                IconButton(
                                    onClick = { plottedPoints = emptyList() },
                                    enabled = plottedPoints.isNotEmpty()
                                ) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear route")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = trailName,
                            onValueChange = { trailName = it },
                            label = { Text("Quest Trail Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_trail_name_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActivityType.entries.forEach { type ->
                                FilterChip(
                                    selected = selectedDiscipline == type,
                                    onClick = { selectedDiscipline = type },
                                    label = { Text(type.displayName) }
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.saveCustomTrail(
                                        name = trailName,
                                        discipline = selectedDiscipline,
                                        points = plottedPoints,
                                        distanceMiles = routeDistanceMiles
                                    )
                                },
                                enabled = plottedPoints.size >= 2,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("save_custom_trail_button")
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Save Trail")
                            }

                            Button(
                                onClick = {
                                    CorosRouteExporter.exportAndShareToCorosWatch(
                                        context = context,
                                        trailName = trailName,
                                        discipline = selectedDiscipline,
                                        points = plottedPoints
                                    ).onSuccess { fileName ->
                                        viewModel.postMessage("Exported $fileName — open in COROS app to sync to watch!")
                                    }.onFailure { err ->
                                        viewModel.postMessage(err.localizedMessage ?: "Export failed.")
                                    }
                                },
                                enabled = plottedPoints.size >= 2,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WolfFrostBlue,
                                    contentColor = Color(0xFF041D26)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_coros_gpx_button")
                            ) {
                                Icon(Icons.Filled.Watch, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Send to COROS", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Saved Custom Quest Trails (Room SQLite)
            item {
                Text(
                    text = "Saved Quest Trails (${savedTrails.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (savedTrails.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tap the map or use '3 mi Loop' to design your first custom trail for your COROS watch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(savedTrails, key = { it.id }) { trail ->
                    val decoded = remember(trail.encodedPoints) {
                        trail.encodedPoints.split("|").mapNotNull { GeoPointEle.decode(it) }
                    }
                    val disc = ActivityType.fromRaw(trail.discipline)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(trail.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "${disc.displayName} · ${SummitFormatters.formatDistance(trail.distanceMiles, unit)} · ${decoded.size} pts",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    plottedPoints = decoded
                                    trailName = trail.name
                                    selectedDiscipline = disc
                                }
                            ) {
                                Text("Load")
                            }
                            IconButton(
                                onClick = {
                                    CorosRouteExporter.exportAndShareToCorosWatch(
                                        context = context,
                                        trailName = trail.name,
                                        discipline = disc,
                                        points = decoded
                                    )
                                }
                            ) {
                                Icon(Icons.Filled.Watch, contentDescription = "Send to COROS Watch", tint = WolfFrostBlue)
                            }
                            IconButton(onClick = { viewModel.deleteCustomTrail(trail.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete trail")
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
