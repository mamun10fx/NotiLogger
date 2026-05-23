# GEMINI.md

## Project Overview

This is a native Android application named "NotiLogger". Its primary function is to capture and log notifications from the Android system. The application saves these notifications to a local database, allowing users to review them later.

The project is written in Kotlin and built with Gradle (using the Kotlin DSL). It uses a standard Android architecture with XML-based views, a background service for data collection, and the Room persistence library for local storage.

**Key Technologies:**
*   **Language:** Kotlin
*   **Platform:** Native Android
*   **Build System:** Gradle with Kotlin DSL (`.kts`)
*   **Core Libraries:**
    *   AndroidX (AppCompat, Core, ConstraintLayout)
    *   Google Material Components
    *   Room Persistence Library (for SQLite database)
    *   Kotlin Coroutines (for background operations)
*   **UI:** Android Views (XML Layouts)

## Building and Running

The project is built using the provided Gradle wrapper (`gradlew`).

### Building the Application

The CI workflow in `.github/workflows/compile.yml` builds a release APK. To build a debug version for development, run the following command from the project's root directory:

```sh
./gradlew assembleDebug
```

To build the release APK as the CI does, run:

```sh
./gradlew assembleRelease
```

The resulting APK file can be found in `app/build/outputs/apk/`.

### Installing and Running on a Device

To build the debug version and automatically install it on a connected Android device or running emulator, use the `installDebug` task:

```sh
./gradlew installDebug
```

After installation, the app requires manual permission to read notifications. This can be granted through `Settings -> Apps -> Special app access -> Notification access`. The app provides a shortcut to this settings page from its navigation menu.

### Running Tests

There are no test files (`/src/test` or `/src/androidTest`) included in the project structure. The standard commands to run tests would be:

```sh
# For local unit tests (JVM)
./gradlew testDebug

# For instrumented tests (on a device/emulator)
./gradlew connectedDebugAndroidTest
```

## Development Conventions

*   **Language & Style:** The codebase is entirely in Kotlin. It follows standard Kotlin conventions, though some code comments are written in Bengali.
*   **Asynchronous Operations:** All database operations are performed on background threads using Kotlin Coroutines (`Dispatchers.IO`) to prevent blocking the UI thread.
*   **Database:** A local SQLite database is managed by the Room library. The schema is defined in the `NotificationEntity.kt` file, and the DAO (Data Access Object) is defined within `AppDatabase.kt`.
*   **Dependency Management:** Dependencies are declared in `app/build.gradle.kts`. The project is not currently using a version catalog (`libs.versions.toml`) for its main dependencies.
*   **Background Service:** A `NotificationListenerService` (`NotificationService.kt`) runs in the background to capture notifications as they are posted by the system.
*   **UI:** The user interface is built with traditional Android XML layouts and Views, including `RecyclerView` for lists. ViewBinding is enabled for safe access to views.
*   **Permissions:** The app requires the `BIND_NOTIFICATION_LISTENER_SERVICE` permission to function, which the user must grant manually from the system settings.
