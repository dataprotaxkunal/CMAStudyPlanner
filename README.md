# CMA Study Planner — Android App (Kotlin + Jetpack Compose)

A full-featured CMA study planner with Pomodoro focus mode, DND integration,
app blocking, schedule management, and revision tracking.

---

## Features

### 📊 Dashboard
- Overall progress percentage and chapter completion stats
- Today's study sessions with quick toggle
- Subject-wise progress bars and study hours

### 📚 Subjects & Chapters
- Add/delete subjects (10 preset CMA subjects for quick add)
- Add/delete chapters per subject
- Mark chapters as done/undone
- Expandable subject cards with progress tracking

### 📅 Calendar
- **Week View** — hourly grid with color-coded study events
- **Month View** — day cells showing schedules and revisions
- Navigation: previous/next week/month, jump to today

### ⏰ Schedule
- Create study sessions (subject, chapter, date, start/end time)
- Filter: All / Today / Upcoming
- Group by date with completion toggle
- Mark sessions complete, delete when done

### 📖 Revision Tracker
- Schedule revisions linked to subjects and chapters
- Overdue / Upcoming / Completed sections
- Add notes to revisions

### 🎯 Focus Mode (Pomodoro Timer)
- Full-screen dark mode timer
- Study and Break phases with configurable durations
- Play / Pause / Stop / Skip controls
- Session counter
- **DND Auto-Toggle** — silences your phone when focus starts
- **App Blocker** — blocks ALL apps except Phone, Messages, Settings, and this app
- Status badges show DND and blocker state

### ⚙️ Settings
- **Permission Setup** — one-tap access to grant DND, Usage Access, and Overlay permissions
- Green/Red status indicators show which permissions are granted
- Toggle DND and App Blocking on/off
- Configure study and break durations
- WhatsApp number for reminders
- List of whitelisted (allowed) apps during focus

---

## How to Build & Install

### Prerequisites
- **Android Studio** (Hedgehog 2023.1.1 or newer)
- **JDK 17** (comes bundled with Android Studio)
- Android phone or emulator running **Android 8.0+ (API 26+)**

### Step-by-Step

1. **Open the project in Android Studio:**
   - Open Android Studio → File → Open → select the `CMAStudyPlanner` folder
   - Wait for Gradle sync to complete (may take 2-3 minutes first time)

2. **If Gradle sync fails:**
   - File → Sync Project with Gradle Files
   - If it asks to update Gradle, accept the defaults
   - If "KSP" is not found, go to File → Settings → Plugins → search "KSP" → install

3. **Connect your phone:**
   - Enable **Developer Options** on your phone:
     Settings → About Phone → tap "Build Number" 7 times
   - Enable **USB Debugging**:
     Settings → Developer Options → USB Debugging → ON
   - Connect phone via USB cable
   - Accept the debugging prompt on your phone

4. **Build & Run:**
   - Select your phone from the device dropdown (top toolbar)
   - Click the green ▶️ Run button (or Shift+F10)
   - The app will build and install on your phone

5. **Generate an APK (to share/install without Android Studio):**
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - APK will be at: `app/build/outputs/apk/debug/app-debug.apk`
   - Transfer this APK to any Android phone and install it

### First Launch — Grant Permissions

When you first open the app, go to **Settings** and grant these 3 permissions:

1. **Do Not Disturb Access** → tap → find "CMA Study Planner" → toggle ON
2. **Usage Access** → tap → find "CMA Study Planner" → toggle ON
3. **Display Over Other Apps** → tap → find "CMA Study Planner" → toggle ON

All three will show green checkmarks when granted.

---

## Project Structure

```
app/src/main/java/com/cma/studyplanner/
├── CMAApp.kt                    — Application class, notification channels
├── MainActivity.kt              — Entry point, bottom navigation, screen routing
├── data/
│   ├── Models.kt                — Room entities (Subject, Chapter, Schedule, Revision)
│   ├── AppDatabase.kt           — Room database + all DAOs
│   └── PrefsManager.kt          — SharedPreferences for settings
├── viewmodel/
│   └── MainViewModel.kt         — All business logic, state management
├── ui/
│   ├── theme/Theme.kt           — Material 3 color scheme
│   ├── components/Components.kt — Reusable UI: StatCard, CheckButton, ScheduleItem, etc.
│   └── screens/
│       ├── DashboardScreen.kt   — Stats overview, today's sessions
│       ├── SubjectsScreen.kt    — Subject/chapter management
│       ├── CalendarScreen.kt    — Week and month calendar views
│       ├── ScheduleScreen.kt    — Session scheduling with filters
│       ├── FocusScreen.kt       — Pomodoro timer (full-screen dark UI)
│       ├── RevisionScreen.kt    — Revision tracker
│       └── SettingsScreen.kt    — Permissions, toggles, WhatsApp config
├── service/
│   ├── FocusService.kt          — Foreground service: DND + app usage monitor
│   └── BlockerActivity.kt       — "Stay Focused!" overlay screen
└── receiver/
    └── Receivers.kt             — Notification reminders, boot receiver
```

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Database:** Room (SQLite)
- **State:** ViewModel + StateFlow + Coroutines
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)
