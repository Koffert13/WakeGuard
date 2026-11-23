# WakeGuard

Ad-free, open, strict wake-up alarm app built natively for **Google Pixel 9 (Android 15 / API 35)** using Kotlin + Jetpack Compose + Material 3.

## Features
- **Zero ads, zero bloat, 100% offline** — focused purely on waking you up reliably.
- **Audio mutes automatically while playing** — tap **Start** to mute the alarm while playing the mini-game. Crashing, going idle for 15 seconds, or trying to leave the app immediately resumes the alarm.
- **Strict Pixel 9 Anti-Exit Guard**:
  - Wakes directly over the lock screen (`USE_FULL_SCREEN_INTENT` + `AlarmManager.setAlarmClock`).
  - Blocks Back gestures/button and hardware Volume keys while ringing, and locks alarm stream volume.
  - Persistent Foreground Service watchdog + Display Over Other Apps (`SYSTEM_ALERT_WINDOW`) immediately relaunches the alarm if backgrounded or swiped away.
  - Optional **Strict Accessibility Guard** (`AlarmGuardAccessibilityService`) intercepts Home, Recents, Settings, Notification Shade, and the Power Menu while an alarm is actively ringing — and stays completely dormant when no alarm is ringing.
  - Persists active ringing alarm across reboots (`BootReceiver` with Direct Boot storage).
- **Modular Mini-Game Missions**:
  - **Snake** (default): Swipe anywhere or use the tactile D-Pad to grow the snake to your target length (configurable length 6–25, speed Chill/Normal/Fast, solid or wrap-around walls).
  - **Math Challenge**: Solve configurable Easy / Medium / Hard arithmetic problems on a lock-screen-safe calculator keypad.
  - **Memory Tiles**: Memorize and recall glowing tile patterns across multiple rounds (`3×3` or `4×4`).
- **Built-in Practice Mode & 5-Second Quick Test**: Test the full alarm lock + game flow right from the home screen without waiting.

## Installing on Your Pixel 9

Google Play Protect on Pixel 9 (Android 15) automatically blocks browser/Files sideloading of any APK that declares an Accessibility Service. Because of this, WakeGuard ships in two builds:

### Option A: Direct Download on Your Pixel 9 — Recommended (`standard` flavor)
1. Download [`apk/WakeGuard-v1.0.1-sideload.apk`](apk/WakeGuard-v1.0.1-sideload.apk) directly on your Pixel 9.
2. Open it in the **Files** app and install normally (no Accessibility Service declared, so Play Protect allows it cleanly).
3. Uses **Display Over Other Apps (`SYSTEM_ALERT_WINDOW`) + 150ms Foreground Watchdog + `onUserLeaveHint` instant snap-back + Back/Volume lock + Lock-screen wake + Reboot survival** to keep you in the game until you win!

### Option B: USB `adb install` with Accessibility Guard (`strict` flavor)
If you also want the extra Accessibility Service (which dismisses the Power Menu & Notification Shade while ringing), install over USB (ADB installs bypass the internet-sideload filter):
```bash
adb install -r apk/WakeGuard-v1.0.1-strict-adb.apk
```

## First-Time Pixel 9 Permission Setup
Open **WakeGuard** and complete the **Setup Pixel 9 Anti-Exit Guard** checklist at the top of the home screen:
1. **Notifications**
2. **Exact Alarms**
3. **Full-Screen Wake**
4. **Display Over Other Apps**
5. **Unrestricted Battery**
6. *(Strict ADB build only)* **Strict Accessibility Guard** — if Android shows *"Restricted setting"*, tap **Open App Info** → top-right **⋮** menu → **Allow restricted settings**, then enable **WakeGuard Anti-Exit Guard**.

## Building & Testing
```bash
./gradlew testDebugUnitTest assembleDebug
```
The installable APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Adding a New Mini-Game
1. Add a new `@Serializable` subtype to `MissionConfig` in `app/src/main/java/com/wakeguard/alarm/model/AlarmModel.kt`.
2. Implement your pure game engine under `app/src/main/java/com/wakeguard/alarm/games/<your_game>/`.
3. Create its Compose UI and wire it in `MissionHost.kt` and `AlarmEditorDialog.kt`.
