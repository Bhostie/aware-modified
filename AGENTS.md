# AGENTS.md — AWARE-light

Guidelines for AI coding agents working in this repository.

## Project Overview

AWARE-light is an Android mobile-sensing research framework. It collects sensor data
(accelerometer, battery, screen, Bluetooth, etc.) via Android Services and stores it
through ContentProviders backed by SQLite. Plugins extend the framework with additional
sensing capabilities (location, activity recognition, ambient noise, etc.).

- **Language**: ~95% Java, ~5% Kotlin (only 4 `.kt` files)
- **Min SDK**: 24 | **Compile/Target SDK**: 28
- **AGP**: 4.1.1 | **Gradle**: 7.2 (Groovy DSL) | **Kotlin**: 1.3.41
- **JDK requirement**: 11 or 17 (NOT 21 — Gradle 7.2 is incompatible with JDK 21)

## Build Commands

```bash
# Ensure JDK 17 is active (required — JDK 21 will fail)
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Full debug build (phone app + all plugins)
./gradlew :aware-phone:assembleDebug

# Build core library only
./gradlew :aware-core:assembleDebug

# Build a single plugin
./gradlew :com.aware.plugin.sentimental:assembleDebug

# Clean build
./gradlew clean assembleDebug

# Lint (configured to never abort — nearly all checks suppressed)
./gradlew :aware-core:lint
./gradlew :aware-phone:lint
```

## Testing

Testing infrastructure is minimal. There is no standard JUnit/Espresso test suite for
the core or phone modules. Tests exist in two forms:

```bash
# 1. Manual test app (Activity-based, runs on device)
./gradlew :aware-tests:installDebug
# Then launch the AWARE Tests app and tap buttons to run individual tests.

# 2. Unit tests in typingtrigger-plugin (placeholder stubs only)
./gradlew :typingtrigger-plugin:test                # Local JUnit tests
./gradlew :typingtrigger-plugin:connectedAndroidTest # Instrumented tests (needs device)

# Run a single test class (if tests are added to a module)
./gradlew :MODULE:test --tests "com.aware.tests.ClassName"
./gradlew :MODULE:test --tests "com.aware.tests.ClassName.methodName"
```

When adding new functionality, prefer adding tests under `src/test/` (local JUnit) or
`src/androidTest/` (instrumented) in the relevant module rather than the `aware-tests` app.

## Module Structure

| Module | Type | Description |
|--------|------|-------------|
| `aware-core` | Library | Core framework: sensors, providers, sync adapters, utilities |
| `aware-phone` | Application | Main phone client UI (`com.aware.phone`) |
| `aware-tests` | Application | Manual test harness app |
| `com.aware.plugin.*` | Libraries | 12 plugin modules (git submodules) |

Dependency graph: `aware-phone` depends on `aware-core` + all plugins.
Each plugin depends on `aware-core`. Do not introduce circular dependencies.

## Architecture

The framework uses a **Service + ContentProvider + SyncAdapter** pattern (not MVVM/MVP):

- **Sensors** extend `Aware_Sensor` (which extends `Service`) — e.g., `Accelerometer.java`
- **Plugins** extend `Aware_Plugin` (which extends `Service`) — e.g., `Plugin.java`
- **Data access** via `ContentProvider` subclasses — e.g., `Accelerometer_Provider.java`
- **Sync** via `AbstractThreadedSyncAdapter` — e.g., `Accelerometer_Sync.java`
- **Inter-component comms** via `BroadcastReceiver` with `ACTION_AWARE_*` intents
- **No DI framework** — dependencies are wired manually; `Context` passed explicitly

Each plugin follows the pattern: `Plugin.java` + `Provider.java` + `Settings.java`
(+ optional `ContextCard.java` and `*_Sync.java`).

## Code Style

### Formatting
- **Indentation**: 4 spaces (no tabs)
- **Brace style**: K&R (opening brace on same line as declaration)
- **Line length**: No strict limit enforced, but aim for ~120 characters
- Single-line `if` without braces is common: `if (x == null) return;`
- No automated formatter is configured (no checkstyle, ktlint, spotless, or editorconfig)

### Naming Conventions
- **Classes**: PascalCase, often with underscores: `Aware_Plugin`, `Accelerometer_Provider`,
  `Accelerometer_Data`. Follow the existing underscore convention for consistency.
- **Methods**: camelCase: `onStartCommand()`, `getFrequency()`, `saveAccelerometerDevice()`
- **Constants** (`static final`): UPPER_SNAKE_CASE: `ACTION_AWARE_ACCELEROMETER`, `DATABASE_VERSION`
- **Static mutable fields**: Also UPPER_SNAKE_CASE by convention: `LAST_VALUES`, `FREQUENCY`
- **Instance fields**: camelCase, sometimes with `m`-prefix: `mSensorManager`, `mContext`
- **Packages**: `com.aware`, `com.aware.utils`, `com.aware.providers`, `com.aware.plugin.<name>`

### Imports
- **Wildcard imports** are used throughout Java files (`android.app.*`, `java.util.*`, etc.)
  — follow the existing pattern in whichever file you are editing.
- **Kotlin files**: Prefer explicit imports.
- **Ordering**: Android/Java SDK first, then `com.aware.*`, then third-party, then `java.*`.
- **No static imports** in production code.

### Types and Nullability
- Java: Always use explicit types. No `@NonNull`/`@Nullable` annotations are used.
- Kotlin: Type inference is fine for locals. Avoid `!!` (non-null assertion) where
  possible — use `?.` safe-call or `?.let { }` instead.
- Use primitives (`int`, `long`, `boolean`) for locals; boxed types when nullability is needed.

### Error Handling
- The codebase consistently uses `try-catch` with `e.printStackTrace()`.
- Conditional debug logging: `if (Aware.DEBUG) Log.d(TAG, e.getMessage())`
- When adding new code, prefer logging over silent `printStackTrace()`:
  ```java
  try {
      // ...
  } catch (JSONException e) {
      if (Aware.DEBUG) Log.w(TAG, "Failed to parse config", e);
  }
  ```
- ContentProviders throw `IllegalArgumentException` for unknown URIs (standard pattern).
- Do NOT use empty catch blocks.

### Async Patterns
- The codebase uses `AsyncTask`, raw `Thread`, `HandlerThread`, and `IntentService`.
- **No coroutines, RxJava, Flow, or LiveData** are present.
- When extending existing code, match the surrounding async pattern.
- Be aware: some `Thread` usages call `.run()` instead of `.start()` (runs synchronously).

### Documentation
- Java: Use Javadoc (`/** ... */`) for public classes and methods with `@author`, `@param`,
  `@return` tags. Many existing methods lack documentation — add it when modifying.
- Kotlin: Use KDoc where applicable.
- Use `// TODO:` comments for deferred work. Existing code has many TODOs.
- Do NOT leave large blocks of commented-out code.

## Key Files and Directories

```
aware-core/src/main/java/com/aware/
  Aware.java              — Main framework Service (start here to understand the system)
  utils/Aware_Sensor.java — Base class for all sensors
  utils/Aware_Plugin.java — Base class for all plugins
  utils/DatabaseHelper.java — SQLite helper
  utils/Http.java / Https.java — Network utilities
  utils/Scheduler.java    — Background task scheduling
  providers/              — All ContentProvider classes
  syncadapters/           — All SyncAdapter classes

aware-phone/src/main/java/com/aware/phone/
  Aware_Client.java       — Main activity
  ui/                     — UI activities and preference screens
```

## Important Constraints

1. **Do not upgrade AGP, Gradle, or Kotlin** without explicit instruction — the project
   uses legacy versions that require careful migration.
2. **Lint is suppressed** for ~146 issues. `lintOptions { abortOnError false }` is set
   everywhere. Do not assume lint will catch problems.
3. **Git submodules**: The 12 plugin directories are git submodules. Run
   `git submodule update --init --recursive` after cloning.
4. **`typingtrigger-plugin`** is commented out in `settings.gradle` — its code was
   migrated into `aware-core`. Do not re-enable it.
5. **No CI/CD** pipeline exists. Verify builds locally before committing.
6. **Pre-built APKs** in `resources/` are for distribution — do not modify them directly.
7. **`aware-thesis.keystore`** is the signing key — do not commit changes to it or
   expose its passwords.
