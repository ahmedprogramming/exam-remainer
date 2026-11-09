# Student Reminder 📚⏰

An offline academic organizer for university and college students. It keeps track of exams (with the chapters you need to study), assignments and deadlines, and reminds you before each one.

**Tech:** Kotlin · Jetpack Compose (Material 3) · Room · DataStore · AlarmManager · WorkManager
**Requires:** Android 8.0 (API 26) or newer. Targets Android 15 (API 35).
**No internet, account or server needed.** Everything is stored on the phone.

---

## Features

| Area | What it does |
|---|---|
| **Home** | Greeting, stat tiles (upcoming exams, pending, completed and overdue assignments), today's events, the next exam with a live days/hours/minutes countdown and chapter progress, the next deadline, overdue work, and an upcoming timeline sorted by date. |
| **Exams** | Add, edit and delete exams with a subject, several chapters (reorderable), date, time, location and notes. Upcoming and Past tabs, automatic date+time sorting, a subject filter, and a detail screen where you tick off chapters as you study them. |
| **Assignments** | Add, edit, complete and delete assignments with a title, subject, description, due date and time, and priority (Low/Medium/High). Overdue, pending and completed items are colour-coded, with filter chips and a one-tap complete button. |
| **Reminders** | Multiple reminders per event: 7/5/3/2/1 days before, on the same day (morning, time configurable), at the exact time, or a custom number of days, hours or minutes. Notifications show the name, date and remaining time. Assignment notifications have **Mark as done** and **Snooze 1 hour** buttons. |
| **Calendar** | Month grid (swipe or use the arrows), dots for exams, assignments and completed items, a Today button, a selected-day schedule, and a button to add an event on the chosen date. |
| **Search & filter** | Searches subjects, titles, descriptions, chapters, locations and notes. Filters by type, status and subject. |
| **Subjects** | Reused automatically (with autocomplete) when you type a subject. You can rename them, change their colour, or delete unused ones. |
| **Settings** | Light, dark or system theme. 6 accent colours plus Material You colours. First day of the week (Sat/Sun/Mon). 12h or 24h time. Notification on/off and default reminders. Permission and battery status with one-tap fixes, a test notification, and backup and restore. |
| **Backup** | Exports everything (subjects, exams, chapters, assignments, reminders, settings) to a JSON file you can keep in Drive or Downloads, or move to a new phone. Restore replaces the current data. |

Delete actions always ask for confirmation. Lists show helpful empty states.

---

## How reminders work (and why they're reliable)

* Each reminder is a row in the Room database and an alarm in `AlarmManager` (`setExactAndAllowWhileIdle`), so it fires in Doze mode and needs no internet.
* **Exact alarms**: the app declares `USE_EXACT_ALARM` (Android 13+) and `SCHEDULE_EXACT_ALARM` (Android 12), so reminders fire on time. If exact alarms are ever blocked, it falls back to a slightly less precise alarm.
* **Notification permission** (Android 13+) is requested on first launch. Its status is shown in Settings.
* **Edit an event**: its old alarms are cancelled and new ones are scheduled. **Delete an event or complete an assignment**: its alarms are cancelled.
* **Phone restart, app update, or a time or time-zone change**: `BootReceiver` uses WorkManager to re-register every alarm from the database. A reminder that should have fired while the phone was off (within the last 12 hours) is shown as soon as the phone is back on, if the event is still ahead.
* As a safety net, a periodic WorkManager job re-checks all alarms every 12 hours. Alarms are also re-checked every time the app starts.
* The notification text is built when the reminder fires, so the remaining time is always accurate.

### Battery optimization ⚠️
Some phone makers (Samsung, Xiaomi/Redmi/POCO, Huawei, Oppo, Vivo, OnePlus, Realme) add aggressive battery savers that can delay or block background alarms. For best results:
1. **Settings → Notifications & reminders → Battery optimization → tap → Allow** ("Unrestricted").
2. Make sure **Notification permission** and **Exact alarms** both show green.
3. Xiaomi/MIUI/HyperOS: enable **Autostart** for Student Reminder. Samsung: remove it from **Sleeping apps / Deep sleeping apps**.
4. Don't use **Force stop** on the app. Android cancels a force-stopped app's alarms until you open it again (opening the app restores them).
5. Use **Send test** in Settings to check that notifications appear.

---

## Getting the APK

### Option A: Let GitHub build it (no Android Studio needed)
1. Create a new repository on GitHub and upload this whole folder (or `git push` it).
2. Open the **Actions** tab. The **Build APK** workflow runs automatically (about 5 minutes).
3. Open the finished run and download the **StudentReminder-apk** artifact (a zip).
4. Inside it is `app-release.apk` (smaller, optimized) and `app-debug.apk`.

### Option B: Android Studio
1. Install **Android Studio** (Ladybug 2024.2 or newer).
2. **File → Open** and select the `StudentReminder` folder. Wait for the Gradle sync (the first one downloads dependencies).
3. Connect your phone (with USB debugging on) and press **Run ▶**, or
   **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. The APK appears in `app/build/outputs/apk/`.

### Option C: Command line
You need JDK 17 and the Android SDK (set `ANDROID_HOME` or create `local.properties` with `sdk.dir=...`).
```bash
./gradlew testDebugUnitTest      # run unit tests
./gradlew assembleRelease        # → app/build/outputs/apk/release/app-release.apk
./gradlew installDebug           # build and install on a connected phone
```

> The release APK is signed with the standard debug key so it installs directly. To publish on Google Play, create your own signing key and update `signingConfig` in `app/build.gradle.kts`.

## Installing on your phone
1. Copy the `.apk` to your phone (USB, Google Drive, Telegram, WhatsApp…), or download it there directly.
2. Tap it. If Android asks, allow **Install unknown apps** for the app you opened it with (Files, Chrome, Drive…).
3. Open **Student Reminder** and tap **Allow** when it asks for notification permission.
4. Go to **Settings** and check that all three status rows (notifications, exact alarms, battery) are green.

> **Updating:** an APK built on a different computer or CI run may have a different signature. If Android says "App not installed", first back up your data (Settings → Back up data), uninstall the old version, install the new one, and restore.

## Using the app
* **Add**: tap the **+** button on Home, Exams, Assignments or Calendar, then choose *New exam* or *New assignment*. On the Calendar, the new event uses the selected date.
* **Exams**: type the subject (pick an existing one from the suggestions), choose the date and time, add chapters with **Add chapter**, pick your reminders and press **Save**. Open an exam to tick off chapters as you study them.
* **Assignments**: tap the circle to mark one as done (tap again to undo). Tap the card to edit or delete it.
* **Notifications**: tap a notification to open the event. On assignment reminders, use **Mark as done** or **Snooze 1 hour**.
* **Search**: use the 🔍 icon in the top bar of any main screen.
* **Backup**: **Settings → Back up data** saves a `.json` file. **Restore from backup** brings it back on any phone.

---

## Project structure
```
app/src/main/java/com/studentreminder/app/
├── StudentReminderApp.kt     Application + AppContainer (manual DI)
├── MainActivity.kt           Compose host, opens screens from notifications
├── data/
│   ├── local/                Room entities, relations, DAOs, AppDatabase
│   ├── StudentRepository.kt  All data operations + reminder scheduling rules
│   ├── SettingsRepository.kt DataStore-backed user preferences
│   ├── BackupManager.kt      JSON export / import via the system file picker
│   └── ReminderOption.kt     Reminder presets, labels, trigger-time maths
├── reminders/
│   ├── ReminderScheduler.kt  AlarmManager wrapper
│   ├── Receivers.kt          Reminder, notification-action and boot receivers
│   ├── RescheduleWorker.kt   WorkManager job that restores alarms
│   └── NotificationHelper.kt Channels and notification building
├── ui/
│   ├── Navigation.kt         Routes, bottom bar, NavHost
│   ├── *ViewModel*.kt        Screen state
│   ├── components/           Cards, pickers, reminder selector, FAB, dialogs
│   ├── screens/              Home, Exams, Assignments, Calendar, Search, Settings, Subjects
│   └── theme/                Material 3 theme, accent palettes, colours
└── util/TimeUtils.kt         Date maths and 12h/24h formatting
app/src/test/                 Unit tests (reminder timing, sorting, formatting)
.github/workflows/            CI that builds the APK
```

## Test checklist
The unit tests cover reminder timing, labels, countdowns and chronological sorting. To check the rest on your phone:
- [ ] Add 3 exams in random order. They should appear sorted by date and time (Exams tab and Home).
- [ ] Edit an exam's date. Its position updates and its reminder dates in the detail screen change.
- [ ] Delete an exam (confirm dialog). It disappears and its reminders are cancelled.
- [ ] Add an assignment due in 3 minutes with a **custom 2 minutes before** reminder. A notification arrives about 1 minute later.
- [ ] Use **Mark as done** from the notification. The assignment moves to Completed.
- [ ] Add an assignment due yesterday. It shows as **Overdue** in red.
- [ ] Close the app (swipe it away) and reopen it. All data is still there.
- [ ] Restart the phone with a reminder scheduled for after the restart. It still arrives.
- [ ] Turn on airplane mode. Everything still works.
- [ ] Back up, delete some data, and restore. Everything comes back.
"# exam-remainer" 
