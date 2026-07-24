# Fix Room Schema Export Warning

The goal is to resolve the KSP warning regarding the Room schema export directory by properly configuring the Room Gradle plugin.

## User Review Required

> [!NOTE]
> This change will create a new `schemas` directory in your `app` module. Room will automatically generate JSON files here whenever the database version or entities change. It is recommended to commit these files to Git.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Android_Projects/Reminera/gradle/libs.versions.toml)
- Add the `androidx-room` plugin definition.

#### [MODIFY] [build.gradle.kts (root)](file:///C:/Android_Projects/Reminera/build.gradle.kts)
- Add the `androidx-room` plugin to the plugins block (with `apply false`).

#### [MODIFY] [app/build.gradle.kts](file:///C:/Android_Projects/Reminera/app/build.gradle.kts)
- Apply the `androidx.room` plugin.
- Configure the `room` extension to specify the schema directory.

## Verification Plan

### Automated Tests
- Run `:app:assembleDebug` to verify that the warning is gone and the schema file is generated.

### Manual Verification
- Check if `app/schemas/com.rekluzlabs.reminera.data.RemineraDatabase/6.json` (or similar) is created.
