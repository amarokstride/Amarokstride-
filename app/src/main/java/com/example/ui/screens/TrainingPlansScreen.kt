package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ActivityType
import com.example.data.model.AdaptiveTrainingPlan
import com.example.data.model.EquestrianIntervalState
import com.example.data.model.UiState
import com.example.ui.theme.SummitGold
import com.example.ui.theme.TrailEquestrianAccent
import com.example.ui.theme.TrailHikeAccent
import com.example.ui.theme.WolfFrostBlue
import com.example.ui.viewmodel.SummitViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TrainingPlansScreen(
    viewModel: SummitViewModel,
    onNavigateBack: () -> Unit
) {
    val plans by viewModel.trainingPlansState.collectAsStateWithLifecycle()
    val isLoading by viewModel.trainingPlanLoading.collectAsStateWithLifecycle()
    val activitiesState by viewModel.activitiesState.collectAsStateWithLifecycle()

    val completedRides = (activitiesState as? UiState.Success)?.data.orEmpty()
        .count { it.typeEnum() == ActivityType.EQUESTRIAN }
    val currentEqStage = remember(completedRides) {
        EquestrianIntervalState.computeForElapsed(0L, completedRides)
    }

    var goalInput by remember { mutableStateOf("Run a 10K under 60 minutes") }
    var selectedDiscipline by remember { mutableStateOf(ActivityType.RUN) }

    val presets = listOf(
        "Run a 10K under 60 minutes" to ActivityType.RUN,
        "Half Marathon 13.1 Alpha Build" to ActivityType.RUN,
        "Hike a steep 12-mile alpine ridge" to ActivityType.HIKE,
        "25-Mile Horse Endurance Conditioning" to ActivityType.EQUESTRIAN
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adaptive Training Plans", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("training_plans_back")) {
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
            // Plan Builder Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, WolfFrostBlue.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = WolfFrostBlue)
                            Text(
                                text = "Forge Personalized AI Training Plan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Powered by Firebase AI Logic & saved locally in Room SQLite. Automatically recalibrates from your COROS, Garmin, Strava, Health Connect & GPS logs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            presets.forEach { (presetGoal, disc) ->
                                SuggestionChip(
                                    onClick = {
                                        goalInput = presetGoal
                                        selectedDiscipline = disc
                                    },
                                    label = { Text(presetGoal, maxLines = 1) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActivityType.entries.forEach { type ->
                                FilterChip(
                                    selected = selectedDiscipline == type,
                                    onClick = { selectedDiscipline = type },
                                    label = { Text(type.displayName) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = goalInput,
                            onValueChange = { goalInput = it },
                            label = { Text("Your Custom Goal") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("training_goal_input")
                        )

                        Button(
                            onClick = { viewModel.generateAndSaveTrainingPlan(goalInput, selectedDiscipline) },
                            enabled = !isLoading && goalInput.isNotBlank(),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_training_plan_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Recalibrating with Watch Data...")
                            } else {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Generate / Recalibrate 7-Day Plan", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Equestrian Progressive Interval Ladder Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, TrailEquestrianAccent.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Equestrian Endurance Progression Ladder",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TrailEquestrianAccent
                        )
                        Text(
                            text = "Current Stage: ${currentEqStage.stageTitle}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Starts with repeating 10 min Walk -> 10 min Trot -> 10 min Canter intervals and progressively extends working trot & canter blocks across 6 marathon-style conditioning stages as you log rides ($completedRides completed).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Saved Plans
            if (plans.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No saved training plans yet. Pick a preset goal or type your own above to forge your 7-day schedule.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(plans, key = { it.id }) { plan ->
                    TrainingPlanCard(
                        plan = plan,
                        onDelete = { viewModel.deleteTrainingPlan(plan.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun TrainingPlanCard(
    plan: AdaptiveTrainingPlan,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.discipline.questLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = WolfFrostBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = plan.goalTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete training plan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = plan.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Watch sync note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Watch, contentDescription = null, tint = TrailHikeAccent, modifier = Modifier.size(16.dp))
                Text(
                    text = plan.watchSyncNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = TrailHikeAccent
                )
            }

            // 7-day schedule
            plan.weeklySchedule.forEach { day ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (day.isRestDay) {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (day.isRestDay) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = day.dayLabel.take(3),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (day.isRestDay) MaterialTheme.colorScheme.onSurfaceVariant else WolfFrostBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = day.workoutTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = day.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (day.isRestDay) {
                            Icon(
                                imageVector = Icons.Filled.SelfImprovement,
                                contentDescription = "Rest Day",
                                tint = TrailHikeAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Rest Day & Motivation footer
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Hotel, contentDescription = null, tint = SummitGold, modifier = Modifier.size(16.dp))
                        Text("Recovery & Motivation", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SummitGold)
                    }
                    Text(plan.restDayGuidance, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "\"${plan.motivationalTip}\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = WolfFrostBlue
                    )
                }
            }
        }
    }
}
