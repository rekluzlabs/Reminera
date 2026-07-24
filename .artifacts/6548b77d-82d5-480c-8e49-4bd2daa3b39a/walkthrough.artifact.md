# Walkthrough - Room Schema Export Setup

I have configured the Room Gradle plugin to handle schema exports, which resolves the KSP warning and provides better database versioning.

## Changes

### Build Configuration

#### [libs.versions.toml](file:///C:/Android_Projects/Reminera/gradle/libs.versions.toml)
- Added the `androidx-room` plugin.

#### [build.gradle.kts (root)](file:///C:/Android_Projects/Reminera/build.gradle.kts)
- Declared the Room plugin.

#### [app/build.gradle.kts](file:///C:/Android_Projects/Reminera/app/build.gradle.kts)
- Applied `androidx.room` plugin.
- Configured `room { schemaDirectory("$projectDir/schemas") }`.

## Verification Results

### Automated Tests
- Ran `:app:assembleDebug`. The build completed successfully without the schema export warning.

### Manual Verification
- Verified that the schema file was generated at: `app/schemas/com.rekluzlabs.reminera.data.RemineraDatabase/6.json`.

> [!TIP]
> You should commit the `app/schemas` directory to your version control system. This allows Room to track schema changes and helps with auto-migrations in the future.
