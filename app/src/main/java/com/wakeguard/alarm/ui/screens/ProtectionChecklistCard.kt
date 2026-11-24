package com.wakeguard.alarm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeguard.alarm.permissions.GuardPermissionsStatus
import com.wakeguard.alarm.ui.theme.WakeAccent
import com.wakeguard.alarm.ui.theme.WakeAmber
import com.wakeguard.alarm.ui.theme.WakeBg
import com.wakeguard.alarm.ui.theme.WakeBorder
import com.wakeguard.alarm.ui.theme.WakeEmerald
import com.wakeguard.alarm.ui.theme.WakeSurface
import com.wakeguard.alarm.ui.theme.WakeSurfaceHigh
import com.wakeguard.alarm.ui.theme.WakeTextMuted
import com.wakeguard.alarm.ui.theme.WakeTextPrimary
import com.wakeguard.alarm.ui.theme.WakeTextSecondary

@Composable
fun ProtectionChecklistCard(
    status: GuardPermissionsStatus,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarms: () -> Unit,
    onOpenFullScreenIntent: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenAppInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember(status.allGranted) { mutableStateOf(!status.allGranted) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WakeSurface)
            .border(1.dp, WakeBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (status.allGranted) WakeEmerald else WakeAmber)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (status.allGranted) {
                            "Exit guard ready"
                        } else {
                            "Finish exit guard setup"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = WakeTextPrimary
                    )
                    Text(
                        text = "${status.grantedCount}/${status.totalCount} permissions active",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = WakeTextMuted
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = WakeTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PermissionRow(
                    title = "Notifications",
                    subtitle = "Required to show active alarm screen",
                    granted = status.notificationsGranted,
                    onGrant = onRequestNotifications
                )
                PermissionRow(
                    title = "Exact alarms",
                    subtitle = "Triggers on the exact second during Doze",
                    granted = status.exactAlarmGranted,
                    onGrant = onOpenExactAlarms
                )
                PermissionRow(
                    title = "Full-screen wake",
                    subtitle = "Opens over the lock screen",
                    granted = status.fullScreenIntentGranted,
                    onGrant = onOpenFullScreenIntent
                )
                PermissionRow(
                    title = "Display over other apps",
                    subtitle = "Only active while ringing · returns to mission if you leave",
                    granted = status.overlayGranted,
                    onGrant = onOpenOverlay
                )
                PermissionRow(
                    title = "Unrestricted battery",
                    subtitle = "Keeps the ringing service running",
                    granted = status.batteryUnrestricted,
                    onGrant = onOpenBattery
                )
                if (status.hasAccessibilityGuard) {
                    PermissionRow(
                        title = "Accessibility guard (optional)",
                        subtitle = "Dismisses Power menu & shade while ringing — may block BankID",
                        granted = status.accessibilityGuardEnabled,
                        onGrant = onOpenAccessibility
                    )

                    if (!status.accessibilityGuardEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(WakeBg)
                                .border(1.dp, WakeBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Leave this off if you use BankID or banking apps that block Accessibility Services. WakeGuard's standard exit guard already protects alarms without it.",
                                fontSize = 12.sp,
                                color = WakeTextSecondary,
                                lineHeight = 18.sp
                            )
                            TextButton(
                                onClick = onOpenAppInfo,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Open App Info", color = WakeAccent, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (granted) WakeBg else WakeSurfaceHigh)
            .border(1.dp, WakeBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (granted) WakeEmerald else WakeTextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = WakeTextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = WakeTextMuted,
                    lineHeight = 16.sp
                )
            }
        }
        if (!granted) {
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onGrant,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = WakeTextPrimary,
                    contentColor = WakeBg
                )
            ) {
                Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
