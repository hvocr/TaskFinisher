# TaskFinisher

An offline-first Android todo app that helps you actually finish your tasks.

## Features

- 📝 Create tasks with deadlines, importance (High/Medium/Low), and energy level
- 📅 Calendar view + date picker for deadlines
- 🔔 Offline notifications (periodic reminders + deadline alerts)
- ⭐ "Today's Big 3" focus mode
- 📊 Streak tracking to keep you consistent
- 🔄 Recurring tasks (daily, weekly, monthly)
- 📁 Archive completed tasks
- 🔍 Filter & search
- 🌙 Dark mode (default)
- ✅ Subtasks support

## Tech Stack

- Kotlin + Jetpack Compose
- Room (SQLite) for offline storage
- WorkManager for notifications
- MVVM architecture

## Requirements

- Android 7.0 (API 24) or higher

## Installation

1. Download the APK from [Releases](../../releases)
2. Enable "Install from unknown sources" on your device
3. Open the APK and install

Or build from source:

```bash
git clone https://github.com/yourusername/TaskFinisher.git
cd TaskFinisher
Open in Android Studio → Run ▶️
