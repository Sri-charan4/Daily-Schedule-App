# Daily Schedule

Fully offline daily planner / routine tracker. No sign-up, no network permission,
no cloud dependency. Backup/restore is done via a plain JSON file the user
controls (share to WhatsApp, Drive, save locally, whatever).

## Opening the project

1. Open Android Studio (Koala or newer recommended).
2. **File > Open** and select the `DailySchedule` folder.
3. Let Gradle sync — it'll pull in the wrapper automatically the first time
   you open it (if it doesn't, run `File > Sync Project with Gradle Files`).
4. Run on an emulator or physical device (minSdk 26 / Android 8.0+).

The launcher icon is an adaptive vector (`drawable/ic_launcher_foreground.xml`
plus `mipmap-anydpi-v26/`), so there are no per-density rasters to regenerate.

## Architecture

- **Kotlin + Jetpack Compose** for UI (Material 3)
- **Room** for local persistence — `ScheduleItem` (tasks + recurring routines
  in one table) and `Completion` (per-day completion tracking for streaks)
- **MVVM**: `ScheduleViewModel` exposes a `StateFlow` of items via
  `ScheduleRepository` → `ScheduleDao`
- **Navigation-Compose** for the 3 screens: Home, Add/Edit, Settings
- **AlarmManager** for exact-time local reminders (no Firebase/push — this
  needs to work with zero network access)

## Backup / Restore (the feature you asked about)

`backup/BackupManager.kt` is the whole flow:

- `createBackupFile()` — serializes every `ScheduleItem` + `Completion` row
  into a single JSON file, written to app cache (`cacheDir/backups/`)
- `buildShareIntent()` — wraps that file in a standard Android share sheet
  via `FileProvider`, so it can go straight to WhatsApp, Drive, email, Bluetooth,
  a USB file transfer app, anything the user has installed
- `restoreFromUri()` — takes a `Uri` from Android's system file picker
  (`ACTION_OPEN_DOCUMENT`, wired up in `SettingsScreen.kt`), parses the JSON,
  wipes the local tables, and re-inserts everything

This means restore works even if the file arrived via WhatsApp, USB transfer,
Google Drive, or manually copied with a file manager — anything that can hand
Android a file `Uri` will work, since it goes through the system picker
rather than a hardcoded folder.

## Reminders

`reminderEnabled` on an item means "nudge me about this". The chain:

- `domain/Occurrences.kt` — `nextReminderAfter()` is the single source of truth
  for *when*. One-off items fire at their own date/time; routines walk forward
  to their next chosen weekday; days that were let go of individually are
  stepped over; an item with no day at all returns null. No time set means
  9:00am (`DEFAULT_REMINDER_TIME`).
- `ReminderScheduler` — books exactly one alarm per item: the next one.
- `ReminderReceiver` — posts the notification, then **books the following
  alarm**. This is what keeps a routine going; there is no repeating alarm.
- `ReminderSync` — rebuilds every alarm from the database. Called on app start,
  after a restore, and by `BootReceiver`.
- `BootReceiver` — rebuilds after reboot, app update, and clock/timezone
  changes, all of which invalidate pending alarms.

Two permissions gate this and both are checked rather than assumed:
`POST_NOTIFICATIONS` (Android 13+, requested when the nudge toggle is switched
on) and exact alarms (Android 12+, granted in system settings). Without the
second, reminders fall back to inexact delivery rather than failing. Settings
shows the state of both.

Timing is covered by unit tests in `app/src/test/.../NextReminderTest.kt`:
`./gradlew testDebugUnitTest`.

## What's left to extend

- No stats screen beyond the Garden.
- Notifications have no "mark as tended" action button — tapping one just opens
  the app.
