package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Privacy Policy & Data Safety",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Striders Howl respects your privacy. All network connections are encrypted in transit over HTTPS, and we never include third-party advertising SDKs or sell personal data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                PolicySection(
                    title = "1. Account & Cloud Sync (Firebase)",
                    body = "When you sign in with Google, your display name, user ID, activity logs, weekly challenges, unlocked milestones, and optional Wolf Pack / leaderboard stats are stored securely in Firebase Cloud Firestore scoped to your account."
                )

                PolicySection(
                    title = "2. Foreground Trail Location (GPS)",
                    body = "Precise location is accessed strictly while you actively track a run, hike, or equestrian ride in the foreground to calculate distance, pace, elevation, and route polylines. Striders Howl never tracks background location."
                )

                PolicySection(
                    title = "3. Google Health Connect & Watch Sync",
                    body = "If you connect Google Health Connect, Striders Howl accesses only Exercise Sessions, Distance, Speed, and Elevation Gained to import watch workouts and optionally export completed trails. Optional COROS, Strava, and Garmin OAuth exchanges are processed securely server-side via Firebase Cloud Functions. You can revoke permissions anytime in Android Health Connect settings."
                )

                PolicySection(
                    title = "4. Optional Sensors & Local Storage",
                    body = "Microphone access is used only when you tap voice trail notes. Bluetooth LE is used only when you actively scan for a watch heart-rate strap. Gear items, custom trails, route analyses, and AI training plans are stored privately on-device in Room SQLite."
                )

                PolicySection(
                    title = "5. AI Coaching (Firebase AI Logic)",
                    body = "When you request AI pace coaching, route insights, or adaptive training plans, only the workout summary metrics you choose to analyze are sent securely via Firebase AI Logic to generate coaching guidance."
                )

                PolicySection(
                    title = "6. Account & Data Deletion",
                    body = "You can permanently delete your Striders Howl account and all associated cloud Firestore records, local Room database entries, cached OAuth sessions, and emergency SOS contacts at any time from Profile -> Privacy & Account -> Delete Account & Data. For privacy or support requests, contact amarokstride@gmail.com."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("privacy_policy_close_button")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun PolicySection(
    title: String,
    body: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
