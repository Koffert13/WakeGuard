package com.wakeguard.alarm.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeguard.alarm.WakeGuardApp
import com.wakeguard.alarm.alarm.AlarmTimeCalculator
import com.wakeguard.alarm.model.AlarmItem
import com.wakeguard.alarm.model.MissionConfig
import com.wakeguard.alarm.permissions.PermissionGuardHelper
import com.wakeguard.alarm.service.AlarmRingingState
import com.wakeguard.alarm.ui.screens.AlarmEditorDialog
import com.wakeguard.alarm.ui.screens.PracticeMissionDialog
import com.wakeguard.alarm.ui.screens.ProtectionChecklistCard
import com.wakeguard.alarm.ui.theme.WakeBg
import com.wakeguard.alarm.ui.theme.WakeBorder
import com.wakeguard.alarm.ui.theme.WakeEmerald
import com.wakeguard.alarm.ui.theme.WakeEmeraldDim
import com.wakeguard.alarm.ui.theme.WakeGuardTheme
import com.wakeguard.alarm.ui.theme.WakeSurface
import com.wakeguard.alarm.ui.theme.WakeSurfaceHigh
import com.wakeguard.alarm.ui.theme.WakeTextPrimary
import com.wakeguard.alarm.ui.theme.WakeTextSecondary

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as WakeGuardApp

        setContent {
            WakeGuardTheme {
                HomeScreen(app = app)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If an alarm is actively ringing and the user somehow opened MainActivity, jump straight back!
        if (AlarmRingingState.isRingingActive) {
            startActivity(
                Intent(this, AlarmActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
        }
    }
}

@Composable
private fun HomeScreen(app: WakeGuardApp) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val alarms by app.repository.alarms.collectAsStateWithLifecycle()

    var permissionStatus by remember { mutableStateOf(PermissionGuardHelper.checkStatus(context)) }
    var editingAlarm by remember { mutableStateOf<Pair<AlarmItem, Boolean>?>(null) }
    var practicingMission by remember { mutableStateOf<MissionConfig?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        permissionStatus = PermissionGuardHelper.checkStatus(context)
    }

    // Re-check permissions every time user returns from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionStatus = PermissionGuardHelper.checkStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val nextScheduled = remember(alarms) {
        alarms
            .filter { it.enabled }
            .map { it to AlarmTimeCalculator.nextTriggerMillis(it) }
            .minByOrNull { it.second }
    }

    Scaffold(
        containerColor = WakeBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingAlarm = AlarmItem() to true },
                containerColor = WakeEmerald,
                contentColor = Color(0xFF042219),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Alarm", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // App Header
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "WakeGuard",
                                style = MaterialTheme.typography.headlineLarge,
                                color = WakeTextPrimary
                            )
                            Text(
                                text = if (nextScheduled != null) {
                                    "Next alarm ${AlarmTimeCalculator.formatTimeUntil(nextScheduled.second)} • ${nextScheduled.first.formattedTime()} ${nextScheduled.first.amPm()}"
                                } else {
                                    "Zero ads • Strict alarm missions"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (nextScheduled != null) WakeEmerald else WakeTextSecondary
                            )
                        }
                    }
                }
            }

            // Pixel 9 Anti-Exit Protection Checklist
            item {
                ProtectionChecklistCard(
                    status = permissionStatus,
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onOpenExactAlarms = {
                        runCatching { context.startActivity(PermissionGuardHelper.exactAlarmSettingsIntent(context)) }
                    },
                    onOpenFullScreenIntent = {
                        runCatching { context.startActivity(PermissionGuardHelper.fullScreenIntentSettingsIntent(context)) }
                    },
                    onOpenOverlay = {
                        runCatching { context.startActivity(PermissionGuardHelper.overlaySettingsIntent(context)) }
                    },
                    onOpenBattery = {
                        runCatching { context.startActivity(PermissionGuardHelper.batteryOptimizationIntent(context)) }
                    },
                    onOpenAccessibility = {
                        runCatching { context.startActivity(PermissionGuardHelper.accessibilitySettingsIntent()) }
                    },
                    onOpenAppInfo = {
                        runCatching { context.startActivity(PermissionGuardHelper.appInfoIntent(context)) }
                    }
                )
            }

            // Alarms list or empty state
            if (alarms.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(WakeSurface)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Alarm,
                            contentDescription = null,
                            tint = WakeTextSecondary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No alarms yet", style = MaterialTheme.typography.titleMedium, color = WakeTextPrimary)
                        Text(
                            "Tap + to add your first Snake alarm.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WakeTextSecondary
                        )
                    }
                }
            } else {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onClick = { editingAlarm = alarm to false },
                        onToggle = { enabled ->
                            val updated = app.repository.setEnabled(alarm.id, enabled) ?: return@AlarmCard
                            if (enabled) {
                                val trigger = app.scheduler.schedule(updated)
                                if (trigger != null) {
                                    Toast.makeText(
                                        context,
                                        "Alarm set for ${AlarmTimeCalculator.formatTimeUntil(trigger)}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                app.scheduler.cancel(updated)
                            }
                        },
                        onQuickTest = {
                            app.scheduler.scheduleQuickTest(alarm.id, delaySeconds = 5)
                            Toast.makeText(
                                context,
                                "Test alarm firing in 5 seconds! Lock your phone or stay here.",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        onPractice = {
                            practicingMission = alarm.mission
                        }
                    )
                }
            }
        }
    }

    val editState = editingAlarm
    if (editState != null) {
        val (alarm, isNew) = editState
        AlarmEditorDialog(
            initialAlarm = alarm,
            isNew = isNew,
            onSave = { saved ->
                app.repository.upsert(saved)
                val trigger = app.scheduler.schedule(saved)
                if (trigger != null) {
                    Toast.makeText(
                        context,
                        "Alarm scheduled ${AlarmTimeCalculator.formatTimeUntil(trigger)}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                editingAlarm = null
            },
            onDelete = if (isNew) null else {
                {
                    app.scheduler.cancel(alarm)
                    app.repository.delete(alarm.id)
                    editingAlarm = null
                }
            },
            onDismiss = { editingAlarm = null }
        )
    }

    val practice = practicingMission
    if (practice != null) {
        PracticeMissionDialog(
            mission = practice,
            onDismiss = { practicingMission = null }
        )
    }
}

@Composable
private fun AlarmCard(
    alarm: AlarmItem,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onQuickTest: () -> Unit,
    onPractice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(WakeSurface)
            .border(
                width = 1.dp,
                color = if (alarm.enabled) WakeEmerald.copy(alpha = 0.4f) else WakeBorder,
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.alpha(if (alarm.enabled) 1f else 0.55f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = alarm.formattedTime(),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = WakeTextPrimary,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alarm.amPm(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WakeTextSecondary,
                        modifier = Modifier.padding(bottom = 7.dp)
                    )
                }
                Text(
                    text = "${alarm.label} • ${alarm.repeatSummary()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WakeTextSecondary
                )
            }

            Switch(
                checked = alarm.enabled,
                onCheckedChange = onToggle
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(WakeEmeraldDim)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = alarm.mission.summary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WakeEmerald
                    )
                }
                if (alarm.strictExitGuard) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Strict Guard On",
                        tint = WakeTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPractice,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SportsEsports,
                        contentDescription = "Practice Game",
                        tint = WakeTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(WakeSurfaceHigh)
                        .clickable(onClick = onQuickTest)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Test in 5s",
                        tint = WakeEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Test 5s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WakeTextPrimary
                    )
                }
            }
        }
    }
}
