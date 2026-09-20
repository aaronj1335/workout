# Workout

A standalone Pixel Watch (Wear OS) app that walks you through a workout one step at a time.

Workouts live in this repo as YAML. A GitHub Action compiles them to a single JSON file and
publishes it on GitHub Pages; the watch fetches that file and caches it so it works offline.

```
workouts/*.yaml  ──(GitHub Action)──▶  workouts.json on GitHub Pages  ──(HTTPS)──▶  watch app
```

Published data: <https://aaronstacy.com/workout/workouts.json>

| Workouts | Preview | Step | Complete |
|---|---|---|---|
| ![Workout list](docs/screenshots/01-workout-list.png) | ![Workout preview](docs/screenshots/02-workout-preview.png) | ![Session step](docs/screenshots/03-session-step.png) | ![Complete](docs/screenshots/04-session-complete.png) |

Tap a workout to see its steps, **Start** to begin, then **Done** once per step. The ring around
the edge tracks how far through you are, and **‹** steps back. Swiping right leaves a workout.
If the watch is out of signal the last downloaded list is used:

| Offline | No data yet |
|---|---|
| ![Offline](docs/screenshots/05-offline-cached.png) | ![Load error](docs/screenshots/06-load-error.png) |

## Adding or editing a workout

Add a file to `workouts/`. The file name is the workout's id, so use a lowercase slug
(`soccer-ladder.yaml` → id `soccer-ladder`).

```yaml
name: Soccer Ladder            # required, shown in the watch list
description: Agility-ladder footwork for soccer   # optional

steps:                         # required, at least one
  - name: Icky shuffle         # required
    reps: 2                    # required, integer >= 1
    notes: Lead with the outside foot   # optional, shown under the rep count
```

Open a pull request: CI validates every file against [`tools/workout.schema.json`](tools/workout.schema.json)
and fails with the offending file and field. Merging to `main` recompiles and redeploys, and the
watch picks it up on the next refresh.

Keep names short — they have to be readable on a watch face at arm's length. The schema caps
workout names at 40 characters and step names at 60.

## Building the data locally

```bash
npm --prefix tools ci
npm --prefix tools run build   # writes dist/workouts.json and dist/index.html
```

`npm --prefix tools run validate` checks the files without writing anything.

## Building the app

Requires Android Studio (for the JDK and Wear OS SDK):

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

By default the app fetches the published Pages URL. To point a debug build at a local copy:

```bash
python3 -m http.server 8000 --directory dist
JAVA_HOME="..." ./gradlew :app:assembleDebug -PworkoutsUrl=http://10.0.2.2:8000/workouts.json
```

(`10.0.2.2` is the host machine as seen from an emulator. Debug builds allow cleartext HTTP;
release builds do not.)

Unit tests cover the catalog parsing, the cache/refresh rules and the session state machine:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest
```

## Running it on an emulator

```bash
sdkmanager --install "system-images;android-35-ext15;android-wear;arm64-v8a"
avdmanager create avd -n pixel_watch5 -d wearos_large_round -k "system-images;android-35-ext15;android-wear;arm64-v8a"
emulator -avd pixel_watch5 -no-snapshot -gpu host
```

That gives a 454x454 round Wear OS 5.1 device, the same size as a 45 mm Pixel Watch. Two things
to know: the Wear OS 7.0 (`android-37.0`) image segfaults during boot with emulator 37.1.11, and
`-no-snapshot` is needed on a fresh AVD or the emulator quits trying to load a snapshot that is
not there. Then `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## One-time setup

GitHub Pages must be enabled for the deploy job to succeed:
**Settings → Pages → Source: GitHub Actions**.
