package com.wakeguard.alarm.permissions

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.wakeguard.alarm.BuildConfig
import com.wakeguard.alarm.service.AlarmGuardAccessibilityService

data class GuardPermissionsStatus(
    val notificationsGranted: Boolean,
    val exactAlarmGranted: Boolean,
    val fullScreenIntentGranted: Boolean,
    val overlayGranted: Boolean,
    val batteryUnrestricted: Boolean,
    val accessibilityGuardEnabled: Boolean,
    val hasAccessibilityGuard: Boolean = BuildConfig.HAS_ACCESSIBILITY_GUARD
) {
    val allGranted: Boolean
        get() = notificationsGranted &&
            exactAlarmGranted &&
            fullScreenIntentGranted &&
            overlayGranted &&
            batteryUnrestricted &&
            (!hasAccessibilityGuard || accessibilityGuardEnabled)

    val grantedCount: Int
        get() {
            val base = listOf(
                notificationsGranted,
                exactAlarmGranted,
                fullScreenIntentGranted,
                overlayGranted,
                batteryUnrestricted
            ).count { it }
            return if (hasAccessibilityGuard && accessibilityGuardEnabled) base + 1 else base
        }

    val totalCount: Int get() = if (hasAccessibilityGuard) 6 else 5
}

object PermissionGuardHelper {

    fun checkStatus(context: Context): GuardPermissionsStatus {
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val exactAlarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val nm = context.getSystemService(NotificationManager::class.java)
        val fullScreenIntentGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            nm.canUseFullScreenIntent()
        } else {
            true
        }

        val overlayGranted = Settings.canDrawOverlays(context)

        val pm = context.getSystemService(PowerManager::class.java)
        val batteryUnrestricted = pm.isIgnoringBatteryOptimizations(context.packageName)

        val accessibilityGuardEnabled = isAccessibilityServiceEnabled(context)

        return GuardPermissionsStatus(
            notificationsGranted = notificationsGranted,
            exactAlarmGranted = exactAlarmGranted,
            fullScreenIntentGranted = fullScreenIntentGranted,
            overlayGranted = overlayGranted,
            batteryUnrestricted = batteryUnrestricted,
            accessibilityGuardEnabled = accessibilityGuardEnabled
        )
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(AccessibilityManager::class.java) ?: return false
        val expectedComponent = ComponentName(context, AlarmGuardAccessibilityService::class.java)
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        if (enabledServices.any {
                val info = it.resolveInfo?.serviceInfo
                info != null && info.packageName == expectedComponent.packageName && info.name == expectedComponent.className
            }
        ) {
            return true
        }
        val flat = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return flat.split(':').any {
            ComponentName.unflattenFromString(it) == expectedComponent
        }
    }

    fun exactAlarmSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            appInfoIntent(context)
        }
    }

    fun fullScreenIntentSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            appInfoIntent(context)
        }
    }

    fun overlaySettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun batteryOptimizationIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun accessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    fun appInfoIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
    }
}
