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

Google Play Protect on Pixel 9 (Android 15) blocks browser/Files sideloading of APKs that declare an Accessibility Service, and **Swedish BankID (and similar banking apps)** refuses to open while any sideloaded Accessibility Service or Wireless/USB Debugging is enabled. Because of this, WakeGuard ships in two builds:

### Option A: Direct Download on Your Pixel 9 — Recommended & BankID-Safe (`standard` flavor)
1. Download [`apk/WakeGuard-v1.0.2-sideload.apk`](apk/WakeGuard-v1.0.2-sideload.apk) directly on your Pixel 9.
2. Open it in the **Files** app and install normally (no Accessibility Service declared, so both Play Protect and BankID work cleanly).
3. Uses **Display Over Other Apps (`SYSTEM_ALERT_WINDOW`, attached only while ringing) + 150ms Foreground Watchdog + `onUserLeaveHint` & window-focus-loss instant unmute/snap-back + Back/Volume lock + Lock-screen wake + Reboot survival** to keep you in the game until you win!
4. Make sure **Wireless Debugging / USB Debugging** is turned off in Developer Options if you previously enabled it—BankID also checks for active debugging.

### Option B: USB `adb install` with Optional Accessibility Guard (`strict` flavor)
If you do **not** use BankID or banking apps that block Accessibility Services and want the extra Accessibility Service (which dismisses the Power Menu & Notification Shade while ringing), install over USB:
```bash
adb install -r apk/WakeGuard-v1.0.2-strict-adb.apk
```

## First-Time Pixel 9 Permission Setup
Open **WakeGuard** and complete the **Finish exit guard setup** checklist at the top of the home screen:
1. **Notifications**
2. **Exact alarms**
3. **Full-screen wake**
4. **Display over other apps** *(overlay anchor is only attached while an alarm is actively ringing)*
5. **Unrestricted battery**
6. *(Strict ADB build only, optional — leave off if you use BankID)* **Accessibility guard**

## Building & Testing
```bash
./gradlew testStandardDebugUnitTest assembleStandardRelease assembleStrictRelease
```
The installable APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Adding a New Mini-Game
1. Add a new `@Serializable` subtype to `MissionConfig` in `app/src/main/java/com/wakeguard/alarm/model/AlarmModel.kt`.
2. Implement your pure game engine under `app/src/main/java/com/wakeguard/alarm/games/<your_game>/`.
3. Create its Compose UI and wire it in `MissionHost.kt` and `AlarmEditorDialog.kt`.
