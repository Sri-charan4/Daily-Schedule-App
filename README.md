# Daily Schedule

Fully offline daily planner / routine tracker. No sign-up, no network permission,
no cloud dependency. Backup/restore is done via a plain JSON file the user
controls (share to WhatsApp, Drive, save locally, whatever).

## Opening the project

1. Open Android Studio (Koala or newer recommended).
2. **File > Open** and select the `DailySchedule` folder.
3. Let Gradle sync — it'll pull in the wrapper automatically the first time
   you open it (if it doesn't, run `File > Sync Project with Gradle Files`).
4. You'll need a launcher icon at `app/src/main/res/mipmap-*/ic_launcher.png` —
   Android Studio's **Image Asset** wizard (right-click `res` > New > Image Asset)
   will generate all densities for you from a single image.
5. Run on an emulator or physical device (minSdk 26 / Android 8.0+).

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

## What's stubbed / left for you to extend

- `BootReceiver.kt` — Android clears `AlarmManager` alarms on reboot; this is
  where you'd re-read all reminder-enabled items and re-schedule them
- App icon (`ic_launcher`) — generate via Android Studio's Image Asset tool
- The Add/Edit screen currently only supports creating new items end-to-end;
  wire up loading an existing item by ID (via `viewModel.allItems`) if you
  want in-place editing rather than delete + recreate
- Stats/streaks screen — `Completion` table is already there to support it,
  just needs a screen and some date-range queries
