package com.wakeguard.alarm.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wakeguard.alarm.model.AlarmItem
import com.wakeguard.alarm.model.GameDifficulty
import com.wakeguard.alarm.model.MathDifficulty
import com.wakeguard.alarm.model.MemoryGridSize
import com.wakeguard.alarm.model.MissionConfig
import com.wakeguard.alarm.model.SnakeSpeed
import com.wakeguard.alarm.ui.theme.WakeBg
import com.wakeguard.alarm.ui.theme.WakeBorder
import com.wakeguard.alarm.ui.theme.WakeCoral
import com.wakeguard.alarm.ui.theme.WakeEmerald
import com.wakeguard.alarm.ui.theme.WakeEmeraldDim
import com.wakeguard.alarm.ui.theme.WakeSurface
import com.wakeguard.alarm.ui.theme.WakeSurfaceHigh
import com.wakeguard.alarm.ui.theme.WakeTextPrimary
import com.wakeguard.alarm.ui.theme.WakeTextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorDialog(
    initialAlarm: AlarmItem,
    isNew: Boolean,
    onSave: (AlarmItem) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val timeState = rememberTimePickerState(
        initialHour = initialAlarm.hour,
        initialMinute = initialAlarm.minute,
        is24Hour = false
    )

    var label by remember { mutableStateOf(initialAlarm.label) }
    var repeatDays by remember { mutableStateOf(initialAlarm.repeatDays) }
    var soundUri by remember { mutableStateOf(initialAlarm.soundUri) }
    var soundTitle by remember { mutableStateOf(initialAlarm.soundTitle) }
    var volumePercent by remember { mutableIntStateOf(initialAlarm.volumePercent) }
    var vibrate by remember { mutableStateOf(initialAlarm.vibrate) }
    var gradualVolume by remember { mutableStateOf(initialAlarm.gradualVolume) }
    var strictGuard by remember { mutableStateOf(initialAlarm.strictExitGuard) }
    var idleTimeout by remember { mutableIntStateOf(initialAlarm.idleTimeoutSeconds) }

    // Mission config state (preserving sub-options when switching tabs)
    var selectedTab by remember {
        mutableIntStateOf(
            when (initialAlarm.mission) {
                is MissionConfig.Snake -> 0
                is MissionConfig.Math -> 1
                is MissionConfig.Memory -> 2
                is MissionConfig.ColorMatch -> 3
                is MissionConfig.NumberOrder -> 4
                is MissionConfig.Sequence -> 5
            }
        )
    }
    var snakeConfig by remember {
        mutableStateOf(
            (initialAlarm.mission as? MissionConfig.Snake) ?: MissionConfig.Snake()
        )
    }
    var mathConfig by remember {
        mutableStateOf(
            (initialAlarm.mission as? MissionConfig.Math) ?: MissionConfig.Math()
        )
    }
    var memoryConfig by remember {
        mutableStateOf(
            (initialAlarm.mission as? MissionConfig.Memory) ?: MissionConfig.Memory()
        )
    }
    var colorMatchConfig by remember {
        mutableStateOf(
            (initialAlarm.mission as? MissionConfig.ColorMatch) ?: MissionConfig.ColorMatch()
        )
    }
    var numberOrderConfig by remember {
        mutableStateOf(
            (initialAlarm.mission as? MissionConfig.NumberOrder) ?: MissionConfig.NumberOrder()
        )
    }
    var sequenceConfig by remember {
        mutableStateOf(
            (initialAlarm.mission as? MissionConfig.Sequence) ?: MissionConfig.Sequence()
        )
    }
    var practicingMission by remember { mutableStateOf<MissionConfig?>(null) }

    val activeMission: MissionConfig = when (selectedTab) {
        0 -> snakeConfig
        1 -> mathConfig
        2 -> memoryConfig
        3 -> colorMatchConfig
        4 -> numberOrderConfig
        else -> sequenceConfig
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                soundUri = uri.toString()
                soundTitle = RingtoneManager.getRingtone(context, uri)?.getTitle(context)
                    ?: "Custom Alarm Sound"
            }
        }
    }

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
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = WakeTextPrimary)
                    }
                    Text(
                        text = if (isNew) "New Alarm" else "Edit Alarm",
                        style = MaterialTheme.typography.titleMedium,
                        color = WakeTextPrimary
                    )
                    Button(
                        onClick = {
                            onSave(
                                initialAlarm.copy(
                                    hour = timeState.hour,
                                    minute = timeState.minute,
                                    label = label.trim().ifEmpty { "Wake Up" },
                                    enabled = true,
                                    repeatDays = repeatDays,
                                    mission = activeMission,
                                    soundUri = soundUri,
                                    soundTitle = soundTitle,
                                    volumePercent = volumePercent,
                                    vibrate = vibrate,
                                    gradualVolume = gradualVolume,
                                    strictExitGuard = strictGuard,
                                    idleTimeoutSeconds = idleTimeout
                                )
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WakeEmerald,
                            contentColor = Color(0xFF042219)
                        )
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }

                // Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Time Input Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(WakeSurface)
                            .padding(top = 18.dp, bottom = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TimeInput(state = timeState)
                    }

                    // Repeat Days
                    SectionCard(title = "Repeat Days") {
                        val days = listOf(
                            1 to "S",
                            2 to "M",
                            3 to "T",
                            4 to "W",
                            5 to "T",
                            6 to "F",
                            7 to "S"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            days.forEach { (calDay, shortLabel) ->
                                val selected = calDay in repeatDays
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) WakeEmerald else WakeSurfaceHigh)
                                        .clickable {
                                            repeatDays = if (selected) {
                                                repeatDays - calDay
                                            } else {
                                                repeatDays + calDay
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shortLabel,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color(0xFF042219) else WakeTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Label
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Alarm Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Mission Picker Card
                    SectionCard(title = "Wake-Up Mini-Game (Alarm Mutes While Playing)") {
                        val gameTabs = listOf(
                            0 to "Snake",
                            1 to "Math",
                            2 to "Memory",
                            3 to "Color Trap",
                            4 to "Tap 1→N",
                            5 to "Echo"
                        )
                        gameTabs.chunked(3).forEachIndexed { rowIdx, rowTabs ->
                            if (rowIdx > 0) Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowTabs.forEach { (idx, name) ->
                                    FilterChip(
                                        selected = selectedTab == idx,
                                        onClick = { selectedTab = idx },
                                        label = {
                                            Text(
                                                text = name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = WakeEmeraldDim,
                                            selectedLabelColor = WakeEmerald
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        when (selectedTab) {
                            0 -> {
                                DifficultyChipRow(
                                    selected = snakeConfig.difficulty,
                                    onSelect = { diff ->
                                        snakeConfig = MissionConfig.Snake.forDifficulty(diff)
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Target Snake Length: ${snakeConfig.targetLength} (eat ${snakeConfig.targetLength - 3} apples)",
                                    color = WakeTextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Slider(
                                    value = snakeConfig.targetLength.toFloat(),
                                    onValueChange = {
                                        snakeConfig = snakeConfig.copy(targetLength = it.roundToInt())
                                    },
                                    valueRange = 5f..20f,
                                    steps = 14
                                )

                                Text(
                                    text = "Speed",
                                    color = WakeTextSecondary,
                                    fontSize = 13.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SnakeSpeed.entries.forEach { spd ->
                                        FilterChip(
                                            selected = snakeConfig.speed == spd,
                                            onClick = { snakeConfig = snakeConfig.copy(speed = spd) },
                                            label = { Text(spd.label) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                ToggleRow(
                                    title = "Wrap-around Walls",
                                    subtitle = "Pass through edges instead of crashing into walls",
                                    checked = snakeConfig.wrapWalls,
                                    onCheckedChange = { snakeConfig = snakeConfig.copy(wrapWalls = it) }
                                )
                            }

                            1 -> {
                                Text(
                                    text = "Difficulty",
                                    color = WakeTextSecondary,
                                    fontSize = 13.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MathDifficulty.entries.forEach { diff ->
                                        FilterChip(
                                            selected = mathConfig.difficulty == diff,
                                            onClick = { mathConfig = mathConfig.copy(difficulty = diff) },
                                            label = { Text(diff.label) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                Text(
                                    text = "Example: ${mathConfig.difficulty.example}",
                                    color = WakeTextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Problems to Solve: ${mathConfig.problemCount}",
                                    color = WakeTextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Slider(
                                    value = mathConfig.problemCount.toFloat(),
                                    onValueChange = {
                                        mathConfig = mathConfig.copy(problemCount = it.roundToInt())
                                    },
                                    valueRange = 1f..10f,
                                    steps = 8
                                )
                            }

                            2 -> {
                                DifficultyChipRow(
                                    selected = memoryConfig.difficulty,
                                    onSelect = { diff ->
                                        memoryConfig = MissionConfig.Memory.forDifficulty(diff)
                                    }
                                )
                                Text(
                                    text = when (memoryConfig.difficulty) {
                                        GameDifficulty.EASY -> "3×3 grid • 2.1s preview"
                                        GameDifficulty.MEDIUM -> "4×4 grid • 1.6s preview"
                                        GameDifficulty.HARD -> "4×4 grid • 1.1s preview • more tiles"
                                    },
                                    color = WakeTextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Rounds to Clear: ${memoryConfig.rounds}",
                                    color = WakeTextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Slider(
                                    value = memoryConfig.rounds.toFloat(),
                                    onValueChange = {
                                        memoryConfig = memoryConfig.copy(rounds = it.roundToInt())
                                    },
                                    valueRange = 2f..6f,
                                    steps = 3
                                )
                                Text(
                                    text = "Grid Size",
                                    color = WakeTextSecondary,
                                    fontSize = 13.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MemoryGridSize.entries.forEach { grid ->
                                        FilterChip(
                                            selected = memoryConfig.gridSize == grid,
                                            onClick = { memoryConfig = memoryConfig.copy(gridSize = grid) },
                                            label = { Text(grid.label) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            3 -> {
                                DifficultyChipRow(
                                    selected = colorMatchConfig.difficulty,
                                    onSelect = { diff ->
                                        colorMatchConfig = colorMatchConfig.copy(difficulty = diff)
                                    }
                                )
                                Text(
                                    text = when (colorMatchConfig.difficulty) {
                                        GameDifficulty.EASY -> "4 colors • Match ink color • 9s timer"
                                        GameDifficulty.MEDIUM -> "5 colors • Match ink color • 5.5s timer"
                                        GameDifficulty.HARD -> "6 colors • Switches between Ink & Word • 3.8s timer"
                                    },
                                    color = WakeTextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Rounds to Clear: ${colorMatchConfig.rounds}",
                                    color = WakeTextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Slider(
                                    value = colorMatchConfig.rounds.toFloat(),
                                    onValueChange = {
                                        colorMatchConfig = colorMatchConfig.copy(rounds = it.roundToInt())
                                    },
                                    valueRange = 4f..15f,
                                    steps = 10
                                )
                            }

                            4 -> {
                                DifficultyChipRow(
                                    selected = numberOrderConfig.difficulty,
                                    onSelect = { diff ->
                                        numberOrderConfig = numberOrderConfig.copy(difficulty = diff)
                                    }
                                )
                                Text(
                                    text = when (numberOrderConfig.difficulty) {
                                        GameDifficulty.EASY -> "3×3 grid (1 to 9) • 3 tries"
                                        GameDifficulty.MEDIUM -> "3×4 grid (1 to 12) • 2 tries"
                                        GameDifficulty.HARD -> "4×4 grid (1 to 16) • Alternates reverse order"
                                    },
                                    color = WakeTextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Rounds to Clear: ${numberOrderConfig.rounds}",
                                    color = WakeTextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Slider(
                                    value = numberOrderConfig.rounds.toFloat(),
                                    onValueChange = {
                                        numberOrderConfig = numberOrderConfig.copy(rounds = it.roundToInt())
                                    },
                                    valueRange = 1f..4f,
                                    steps = 2
                                )
                            }

                            else -> {
                                DifficultyChipRow(
                                    selected = sequenceConfig.difficulty,
                                    onSelect = { diff ->
                                        sequenceConfig = sequenceConfig.copy(difficulty = diff)
                                    }
                                )
                                Text(
                                    text = when (sequenceConfig.difficulty) {
                                        GameDifficulty.EASY -> "4 pads • Slow playback • Starts at 3 steps"
                                        GameDifficulty.MEDIUM -> "4 pads • Normal playback • Starts at 4 steps"
                                        GameDifficulty.HARD -> "6 pads • Fast playback • Starts at 4 steps"
                                    },
                                    color = WakeTextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Rounds to Clear: ${sequenceConfig.rounds}",
                                    color = WakeTextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Slider(
                                    value = sequenceConfig.rounds.toFloat(),
                                    onValueChange = {
                                        sequenceConfig = sequenceConfig.copy(rounds = it.roundToInt())
                                    },
                                    valueRange = 2f..6f,
                                    steps = 3
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { practicingMission = activeMission },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Practice ${activeMission.displayName}")
                        }
                    }

                    // Sound & Guard Settings
                    SectionCard(title = "Sound & Anti-Exit Guard") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(WakeSurfaceHigh)
                                .clickable {
                                    val pickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        soundUri?.let {
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                                        }
                                    }
                                    ringtoneLauncher.launch(pickerIntent)
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = WakeEmerald)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Alarm Sound", fontSize = 13.sp, color = WakeTextSecondary)
                                    Text(soundTitle, color = WakeTextPrimary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Text("Change", color = WakeEmerald, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Alarm Volume: $volumePercent%",
                            color = WakeTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = volumePercent.toFloat(),
                            onValueChange = { volumePercent = it.roundToInt() },
                            valueRange = 20f..100f
                        )

                        ToggleRow(
                            title = "Vibrate",
                            subtitle = "Pauses while playing the mini-game",
                            checked = vibrate,
                            onCheckedChange = { vibrate = it }
                        )
                        ToggleRow(
                            title = "Gradual Volume Fade-In",
                            subtitle = "Gently ramps up over 25 seconds",
                            checked = gradualVolume,
                            onCheckedChange = { gradualVolume = it }
                        )
                        ToggleRow(
                            title = "Strict Anti-Exit & Volume Lock",
                            subtitle = "Blocks Home/Recents/Power menu & pins volume until solved",
                            checked = strictGuard,
                            onCheckedChange = { strictGuard = it }
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Resume Alarm if Idle: ${idleTimeout}s",
                            color = WakeTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = idleTimeout.toFloat(),
                            onValueChange = { idleTimeout = it.roundToInt() },
                            valueRange = 10f..30f,
                            steps = 3
                        )
                    }

                    if (onDelete != null) {
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WakeCoral)
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Alarm", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
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
private fun DifficultyChipRow(
    selected: GameDifficulty,
    onSelect: (GameDifficulty) -> Unit
) {
    Text(
        text = "Difficulty",
        color = WakeTextSecondary,
        fontSize = 13.sp
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GameDifficulty.entries.forEach { diff ->
            FilterChip(
                selected = selected == diff,
                onClick = { onSelect(diff) },
                label = { Text(diff.label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(WakeSurface)
            .border(1.dp, WakeBorder, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = WakeEmerald
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = WakeTextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, color = WakeTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
