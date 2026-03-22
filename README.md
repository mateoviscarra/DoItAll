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
**Per-session edits** (sets, single weight, reps single or per-set, comments up to 128 chars,
last-opened carousel page per exercise slot, and which exercise is selected per carousel page)
are persisted with **Jetpack DataStore** (`WorkoutStateStore`).

## Next iterations

- Add workout history and progression tracking over time
- Allow editing the exercise catalog (currently hardcoded per slot)
- Build Android UI and workflows
