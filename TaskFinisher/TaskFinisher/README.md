# TaskFinisher 📋

An offline-first, dark-mode-only Android todo app built with Jetpack Compose, Room, WorkManager, and Hilt.

---

## Features

| Area | What's included |
|------|----------------|
| **Today Screen** | Big 3 tasks (starred), streak counter, all today's tasks |
| **All Tasks** | Search, 7 filter chips, 3 sort modes |
| **Calendar** | Custom month grid with task dots, day detail bottom list |
| **Archive** | Completed/archived tasks, date filters, clear archive |
| **Recurrence** | Daily / Weekly (specific days) / Monthly (day of month) / Custom interval |
| **Big 3** | Star any task; max 3 enforced with Snackbar feedback |
| **Streak** | Consecutive days with ≥1 completion, displayed as 🔥 pill |
| **Notifications** | Exact alarm 1 hour before deadline + daily WorkManager summary |
| **Subtasks** | Inline add/toggle/delete inside Add/Edit dialog |
| **Reschedule** | "Push to Tomorrow" via long-press context menu |

---

## Tech Stack

- **Kotlin** — 100%
- **Jetpack Compose + Material 3** — UI
- **Room (SQLite)** — local persistence, version 1 schema
- **Hilt** — dependency injection
- **WorkManager** — daily reminder worker
- **AlarmManager** — per-task exact deadline alarms
- **StateFlow + ViewModel** — reactive state management
- **java.time** — date/time (desugared for API 24–25)
- **KSP** — annotation processing (Room, Hilt)

---

## Project Structure

```
app/src/main/java/com/taskfinisher/
├── MainActivity.kt
├── TaskFinisherApp.kt          ← Hilt application + WorkManager config
├── di/
│   └── AppModule.kt            ← Hilt Room + DAO providers
├── data/
│   ├── model/Task.kt           ← Domain model (Importance/Energy enums)
│   ├── database/
│   │   ├── TaskEntity.kt       ← Room entity + toDomain/toEntity mappers
│   │   ├── TaskDao.kt          ← All queries (Flow + suspend)
│   │   └── TaskDatabase.kt     ← RoomDatabase, version 1
│   └── repository/
│       └── TaskRepository.kt   ← Business logic: complete, recur, Big 3, streak
├── viewmodel/
│   ├── TodayViewModel.kt
│   ├── AllTasksViewModel.kt
│   ├── CalendarViewModel.kt
│   ├── ArchiveViewModel.kt
│   └── AddEditTaskViewModel.kt
├── notifications/
│   ├── NotificationScheduler.kt  ← AlarmManager exact alarms
│   ├── NotificationReceiver.kt   ← BroadcastReceiver (alarm + boot)
│   └── ReminderWorker.kt         ← Daily WorkManager worker
├── ui/
│   ├── theme/Theme.kt            ← Dark-only color scheme, typography
│   ├── navigation/NavGraph.kt    ← Bottom nav + FAB + screen routing
│   ├── screens/
│   │   ├── TodayScreen.kt
│   │   ├── AllTasksScreen.kt
│   │   ├── CalendarScreen.kt
│   │   └── ArchiveScreen.kt
│   └── components/
│       ├── TaskCard.kt           ← Reusable card with long-press menu
│       ├── AddTaskDialog.kt      ← Full add/edit bottom sheet
│       ├── ImportancePicker.kt
│       ├── EnergyPicker.kt       ← (in ImportancePicker.kt file)
│       ├── RecurrencePicker.kt
│       ├── SubtaskList.kt
│       └── FilterBar.kt
└── utils/
    ├── RecurrenceHelper.kt       ← Parse/build/describe RRULE strings
    ├── StreakCalculator.kt       ← Consecutive-day streak algorithm
    └── Extensions.kt            ← Long.formatDeadline(), LocalDate helpers, newId()
```

---

## Setup Instructions

### 1. Prerequisites

- **Android Studio Hedgehog (2023.1.1)** or newer
- **JDK 17** (bundled with recent Android Studio)
- **Android SDK** with API 34 platform + Build Tools 34

### 2. Clone & Open

```bash
git clone https://github.com/your-org/taskfinisher.git
cd taskfinisher
```

Open in Android Studio: **File → Open → select the `taskfinisher` folder**.

### 3. Sync Gradle

Android Studio will prompt to sync. Click **Sync Now**, or run:

```bash
./gradlew dependencies
```

The version catalog (`gradle/libs.versions.toml`) pins all dependencies. If any resolution error occurs, check that you have the **Google Maven** and **Maven Central** repositories available.

### 4. Run on Device / Emulator

- Target **API 24+** (minSdk = 24)
- For exact alarm testing, use a **physical device** running API 31+ and grant `SCHEDULE_EXACT_ALARM` permission via Settings → Apps → Special app access → Alarms & reminders
- Run: `./gradlew installDebug` or press ▶ in Android Studio

### 5. Notification Permission (API 33+)

On first launch on Android 13+, the app needs `POST_NOTIFICATIONS` permission. The system dialog will appear automatically when WorkManager schedules the daily reminder. No explicit permission request code is included (add `AccompanistPermissions` if you want a rationale dialog before the system prompt).

---

## Recurrence Rule Format

TaskFinisher uses a simplified iCalendar RRULE subset stored as a plain string:

| Pattern | Example Rule String |
|---------|---------------------|
| Every day | `FREQ=DAILY;INTERVAL=1` |
| Every 3 days | `FREQ=CUSTOM;INTERVAL=3` |
| Every week on Mon/Wed/Fri | `FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE,FR` |
| Every 2 weeks on Monday | `FREQ=WEEKLY;INTERVAL=2;BYDAY=MO` |
| Monthly on the 15th | `FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=15` |

When a recurring task is **completed**:
1. The current instance is marked `isCompleted=true, isArchived=true` (moves to archive).
2. A new instance is spawned with `deadline = nextOccurrence(rule, completedDeadline)`.
3. The new instance carries the same `recurrenceRule` so the cycle continues.

---

## Adding a Database Migration

When you add a new column (e.g., `priority: Int`):

1. Increment `version` in `TaskDatabase.kt` to `2`.
2. Add a migration object:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE tasks ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
    }
}
```

3. Pass it to the builder in `AppModule.kt`:

```kotlin
Room.databaseBuilder(...)
    .addMigrations(MIGRATION_1_2)
    .build()
```

The Room schema JSON exported to `/app/schemas/` acts as a diff-friendly snapshot for code review.

---

## Third-Party Library Justifications

| Library | Why |
|---------|-----|
| **Hilt** | Reduces DI boilerplate vs manual injection; official Android recommendation; integrates with WorkManager via `@HiltWorker` with zero extra setup |
| **KSP** (over KAPT) | 2× faster annotation processing; Room and Hilt both support it as of their current versions |
| **WorkManager** | The correct API for deferrable, guaranteed background work on all API levels; replaces `JobScheduler`/`FirebaseJobDispatcher` |
| **Compose Material 3** | Modern, themeable components; `DatePicker`, `TimePicker`, `ModalBottomSheet` are all stable in M3 |
| **Desugar JDK libs** | Backports `java.time` to API 24–25 without any code changes; zero runtime overhead on API 26+ |

No third-party calendar library is used — the calendar grid is custom Compose code (~80 lines) which keeps the APK slim and gives full styling control.

---

## Customization Tips

- **Accent color**: Change `Accent` in `Theme.kt` — one constant flows through the entire UI.
- **Daily reminder time**: Change `hourOfDay`/`minute` in `MainActivity.onCreate`, or wire up a Settings screen that calls `ReminderWorker.scheduleDailyReminder(context, h, m)`.
- **Reminder lead time**: Change `REMINDER_OFFSET_MS` in `NotificationScheduler` (default: 1 hour).
- **Big 3 limit**: Change the `>= 3` check in `TaskRepository.toggleBig3`.

---

## Known Limitations / Future Work

- No Settings screen (daily reminder time is hardcoded to 9:00 AM in `MainActivity`).
- No widget support.
- Streak resets if the device clock is changed manually.
- `SCHEDULE_EXACT_ALARM` is declared but the app doesn't navigate the user to the system permission screen on API 31+ — add Accompanist Permissions + deep link to `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` if needed.
- Calendar dots do not distinguish between overdue and future tasks on months other than the current one.
