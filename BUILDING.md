# Building AWARE-light

This project currently uses an older Android Gradle Plugin (AGP 4.1.1) and Kotlin 1.3.x. Your sync error:

```
Your build is currently configured to use incompatible Java 21.0.7 and Gradle 7.2.
The minimum compatible Gradle version is 8.5. The maximum compatible Gradle JVM version is 17.
```

happens because Gradle 7.2 does not support running on Java 21.

## Recommended Quick Fix (Least Effort)
Use an older JDK (11 or 17) for the build instead of upgrading the entire toolchain right now.

AGP 4.1.1 officially supports Gradle 6.5–6.7.1 and JDK 8–11. In practice it can often still run with JDK 17, but **not with JDK 21**.

So the fastest way to get the project syncing is:
1. Install JDK 17 (or JDK 11) if you don't already have it.
2. Tell Android Studio / Gradle to use that JDK.

### macOS: Install Temurin JDK 17 (recommended)
Using Homebrew:
```bash
brew install --cask temurin17
```
Check its path:
```bash
/usr/libexec/java_home -v 17
```
Export temporarily for CLI builds:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
```
Then run:
```bash
./gradlew --version
```
You should now see something like:
```
Gradle 7.2
JVM: 17.x.y (Eclipse Adoptium ...)
```

### Configure Android Studio
Settings (Preferences on macOS) > Build, Execution, Deployment > Build Tools > Gradle:
- Gradle JDK: Select the installed JDK 17 (NOT JDK 21).

### (Optional) Pin JDK in `gradle.properties`
If you want to always use JDK 17 for this project, add (adjust the path you get from `java_home`):
```
org.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
```
Do **NOT** commit a machine‑specific path if collaborators use different setups; instead you can keep it only in your local `~/.gradle/gradle.properties`.

## Alternative: Roll Back Wrapper to Match AGP Exactly
If you run into unexpected issues with Gradle 7.2 + AGP 4.1.1, you can align with the officially supported combination:
- Change `gradle/wrapper/gradle-wrapper.properties` distributionUrl to `gradle-6.7.1-bin.zip`.
- Still use JDK 11 (or 8). (JDK 17 is not supported by Gradle 6.7.1.)

However, if 7.2 already worked for you previously (aside from the JDK 21 issue), you can leave it.

## Big Upgrade Path (High Effort, Optional)
If you eventually want to use JDK 21, you'd need to modernize:
1. Upgrade Gradle wrapper to 8.5+.
2. Upgrade AGP to at least 8.x (requires JDK 17 minimum; AGP 8.5+ better).
3. Upgrade Kotlin to 1.9.x.
4. Raise `compileSdkVersion` and `targetSdkVersion` (e.g. 34 or 35) and remove deprecated configurations (e.g. `buildToolsVersion` is no longer needed).
5. Migrate any legacy support library usages (you already use AndroidX mostly, but `com.android.support:support-v4:28.0.0` must be removed or replaced with AndroidX equivalents).

This is a non-trivial migration that may require code changes (namespaces, API changes, manifest updates, dependency alignment, desugaring adjustments).

## Summary Matrix
| Path | Effort | Change Needed | JDK | Notes |
|------|--------|---------------|-----|-------|
| Quick Fix | Low | Just select older JDK | 11 or 17 | Fastest, no code changes |
| Roll Back Wrapper | Low/Medium | Set Gradle to 6.7.1 + JDK 11 | 11 | Matches AGP support matrix |
| Full Modernization | High | Upgrade Gradle/AGP/Kotlin/SDK | 17→21 | Enables current tooling |

## Troubleshooting
- Still seeing Java 21 in `./gradlew --version`: Ensure your shell exported JAVA_HOME before running, and that no `.sdkmanrc` / shell init is overriding it.
- Android Studio keeps switching JDK: Re-open Gradle settings and explicitly pick the correct JDK; invalidate caches if necessary.
- Classpath / dependency errors after switching JDK: Clean build (`./gradlew clean assembleDebug`). Some cached incremental compilation artifacts may be incompatible.

## Verification Commands
```bash
# Verify JDK in use
java -version

# Verify Gradle sees correct JVM
./gradlew --version

# Test assemble
./gradlew :aware-phone:assembleDebug
```

## Questions / Next Steps
If you want help with the full modernization, let me know and we can stage the upgrade safely (wrapper -> AGP -> Kotlin -> dependencies -> code cleanup).

---
Maintained guidance added on 2025-10-07.

