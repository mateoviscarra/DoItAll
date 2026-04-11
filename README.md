# DoItAll

Android workout tracker with productivity timers and Google Calendar sync.

## Features

### Workout Tracking
- Hardcoded training plans (Push/Pull/Legs/Accessory)
- Shows muscle groups trained each day
- Alternative exercises that target similar muscles (carousel navigation)
- Per-exercise logging: sets, reps, weight, comments
- Bodyweight exercise support with optional added weight
- Unit toggle: kg or lbs

### Timers & Productivity
- **Timer types**: Productivity (60 min), Reading (60 min), Rest (2 min)
- Productivity timer quick options: Guitar, Programming, Applying
- Custom duration option
- Long vibration toggle on completion
- Notification on completion
- **Only to Calendar mode**: Instantly log a session to Google Calendar without running a timer
- When running a timer, calendar event is created at start time (not completion)
- Rest timers do not create calendar events

### Google Calendar Integration
- Sync workouts to Google Calendar
- Custom calendar selection
- Calendar events include exercise details, weights, and reps
- Secure credential storage on device

### Data Persistence
- Exercise logs saved per day with shared exercise catalog
- Carousel bindings (which exercise per slot) stored per day
- Timer state persists across app restarts

## Data Source

Workout plan stored in `data/workout_plan.json`. On device, copied to `app/src/main/assets/workout_plan.json`.

The app builds a **shared exercise catalog**: the same display name (case-insensitive) maps to one **stable id**, so a movement that appears on multiple days shares **one saved log** (sets, weight, reps, comment). Carousel **bindings** (which id each page uses) and **last page index** per slot are stored **per day** in DataStore (`WorkoutStateStore`, schema v2 key `workout_log_json_v2`).

## Tech Stack
- Kotlin + Jetpack Compose
- DataStore for persistence
- Google Calendar API
- Material Design 3