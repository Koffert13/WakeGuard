package com.wakeguard.alarm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wakeguard.alarm.model.MissionConfig
import com.wakeguard.alarm.ui.games.MissionHost
import com.wakeguard.alarm.ui.theme.WakeBg
import com.wakeguard.alarm.ui.theme.WakeCoral
import com.wakeguard.alarm.ui.theme.WakeEmerald
import com.wakeguard.alarm.ui.theme.WakeTextPrimary
import com.wakeguard.alarm.ui.theme.WakeTextSecondary

@Composable
fun PracticeMissionDialog(
    mission: MissionConfig,
    onDismiss: () -> Unit
) {
    var resetSignal by remember { mutableIntStateOf(0) }
    var bannerText by remember { mutableStateOf<String?>(null) }
    var won by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = WakeBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = "Practice: ${mission.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = WakeTextPrimary
                        )
                        Text(
                            text = "No alarm sound in practice mode",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WakeTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = WakeTextPrimary)
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    MissionHost(
                        mission = mission,
                        paused = bannerText != null || won,
                        resetSignal = resetSignal,
                        onInteraction = {},
                        onCrash = { reason -> bannerText = reason },
                        onMissionComplete = { won = true }
                    )

                    if (bannerText != null || won) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xD90B1016))
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (won) "Mission Cleared!" else "Crashed!",
                                style = MaterialTheme.typography.headlineLarge,
                                color = if (won) WakeEmerald else WakeCoral
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (won) {
                                    "In a real alarm, this immediately stops the alarm."
                                } else {
                                    "${bannerText}\n(In a real alarm, audio resumes here!)"
                                },
                                color = WakeTextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        bannerText = null
                                        won = false
                                        resetSignal++
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WakeEmerald,
                                        contentColor = Color(0xFF042219)
                                    )
                                ) {
                                    Text("Play Again", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Done")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
