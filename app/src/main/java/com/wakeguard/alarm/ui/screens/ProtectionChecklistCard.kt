package com.wakeguard.alarm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeguard.alarm.permissions.GuardPermissionsStatus
import com.wakeguard.alarm.ui.theme.WakeAmber
import com.wakeguard.alarm.ui.theme.WakeBorder
import com.wakeguard.alarm.ui.theme.WakeEmerald
import com.wakeguard.alarm.ui.theme.WakeEmeraldDim
import com.wakeguard.alarm.ui.theme.WakeSurface
import com.wakeguard.alarm.ui.theme.WakeSurfaceHigh
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
            .clip(RoundedCornerShape(22.dp))
            .background(WakeSurface)
            .border(
                width = 1.dp,
                color = if (status.allGranted) WakeEmerald.copy(alpha = 0.45f) else WakeAmber.copy(alpha = 0.45f),
                shape = RoundedCornerShape(22.dp)
            )
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
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = if (status.allGranted) WakeEmerald else WakeAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (status.allGranted) {
                            "Pixel 9 Anti-Exit Guard Ready"
                        } else {
                            "Setup Pixel 9 Anti-Exit Guard"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = WakeTextPrimary
                    )
                    Text(
                        text = "${status.grantedCount} / ${status.totalCount} protections enabled",
                        fontSize = 12.sp,
                        color = if (status.allGranted) WakeEmerald else WakeAmber
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = WakeTextSecondary
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PermissionRow(
                    title = "Notifications",
                    subtitle = "Required to fire active alarm screen",
                    granted = status.notificationsGranted,
                    onGrant = onRequestNotifications
                )
                PermissionRow(
                    title = "Exact Alarms",
                    subtitle = "Rings on the exact second even in Doze",
                    granted = status.exactAlarmGranted,
                    onGrant = onOpenExactAlarms
                )
                PermissionRow(
                    title = "Full-Screen Wake",
                    subtitle = "Pops straight over the Pixel 9 lock screen",
                    granted = status.fullScreenIntentGranted,
                    onGrant = onOpenFullScreenIntent
                )
                PermissionRow(
                    title = "Display Over Other Apps",
                    subtitle = "Instantly pulls you back if you swipe home",
                    granted = status.overlayGranted,
                    onGrant = onOpenOverlay
                )
                PermissionRow(
                    title = "Unrestricted Battery",
                    subtitle = "Prevents Pixel from pausing the ringing service",
                    granted = status.batteryUnrestricted,
                    onGrant = onOpenBattery
                )
                PermissionRow(
                    title = "Strict Accessibility Guard",
                    subtitle = "Blocks Home, Recents, & Power Menu while ringing",
                    granted = status.accessibilityGuardEnabled,
                    onGrant = onOpenAccessibility
                )

                if (!status.accessibilityGuardEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(WakeSurfaceHigh)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Pixel 9 sideload tip: If Accessibility says \"Restricted setting\", tap App Info → top-right ⋮ menu → \"Allow restricted settings\", then enable WakeGuard Anti-Exit Guard.",
                            fontSize = 12.sp,
                            color = WakeTextSecondary,
                            lineHeight = 17.sp
                        )
                        TextButton(
                            onClick = onOpenAppInfo,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Open App Info", color = WakeEmerald, fontWeight = FontWeight.SemiBold)
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
            .clip(RoundedCornerShape(14.dp))
            .background(if (granted) WakeEmeraldDim.copy(alpha = 0.45f) else WakeSurfaceHigh)
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
                tint = if (granted) WakeEmerald else WakeAmber,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WakeTextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = WakeTextSecondary,
                    lineHeight = 14.sp
                )
            }
        }
        if (!granted) {
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onGrant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
