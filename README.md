# DoItAll

Starter project for an Android app that will eventually include multiple modules.

## Current scope

The first module is a workout tracker with hardcoded training plans.

The app should answer:

- Which muscle group(s) are trained today, or whether it is a rest day
- Which exercises to perform, with sets and reps
- Which alternative exercises train similar muscles

## Data source

The initial workout plan is stored in `data/workout_plan.json`.
Notes are standardized to English while preserving progressive overload context
(up, same, down) and practical notes.

On device, the Android app copies this to `app/src/main/assets/workout_plan.json`.

When loading, the app builds a **shared exercise catalog**: the same display name (case-insensitive)
maps to one **stable id**, so a movement that appears on multiple days shares **one saved log**
(sets, weight, reps, comment). Carousel **bindings** (which id each page uses) and **last page index**
per slot are still stored **per day** in DataStore (`WorkoutStateStore`, schema v2 key
`workout_log_json_v2`). Older saved data under the previous key is not migrated.

## Next iterations

- Add workout history and progression tracking over time
- Allow editing the exercise catalog (currently hardcoded per slot)
- Build Android UI and workflows
