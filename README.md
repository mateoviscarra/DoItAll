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

## Next iterations

- Add workout history and progression tracking over time
- Move from hardcoded data to storage
- Build Android UI and workflows
