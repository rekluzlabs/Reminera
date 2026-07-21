# Implementation Plan: Splash Screen & Media Capture

The user wants to:
1.  Add a Splash Screen that displays `reminera_mainimage.webp` for 3 seconds on startup.
2.  Enable recording/importing of video and audio memories.

## User Review Required

> [!IMPORTANT]
> - The Splash Screen will be implemented as a state in `MainActivity` or using a dedicated `SplashScreen` composable with a delay.
> - Media capture will use system intents for simplicity and reliability.
> - Audio recording will use an in-app `MediaRecorder` implementation.

## Proposed Changes

### Navigation & Entry Flow

#### [MODIFY] [MainActivity.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/example/reminera/MainActivity.kt)
- Add a `SplashState` (or similar) to manage the transition from Splash to Home.
- Implement the 3-second delay using `LaunchedEffect`.

#### [NEW] [RemineraSplashScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/example/reminera/ui/splash/RemineraSplashScreen.kt)
- Create a simple composable to display the `reminera_mainimage.webp` image.

### Configuration & Permissions (Media)

#### [MODIFY] [AndroidManifest.xml](file:///C:/Android_Projects/Reminera/app/src/main/AndroidManifest.xml)
- Add `CAMERA` and `RECORD_AUDIO` permissions.
- Add `FileProvider` for URI sharing.

#### [NEW] [file_paths.xml](file:///C:/Android_Projects/Reminera/app/src/main/res/xml/file_paths.xml)
- Define paths for `FileProvider`.

### Media Features

#### [MODIFY] [RemineraHomeScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/example/reminera/ui/home/RemineraHomeScreen.kt)
- Update "Add Memory" sheet to support capture/import for all media types.
- Add audio recording UI.

#### [MODIFY] [RemineraViewModel.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/example/reminera/ui/home/RemineraViewModel.kt)
- Implement file saving and persistence logic.

## Verification Plan

### Automated Tests
- Unit tests for the memory insertion logic in `RemineraViewModel`.

### Manual Verification
- Verify Splash Screen shows for exactly 3 seconds on app launch.
- Verify photo/video capture opens the camera app.
- Verify gallery import works for images and videos.
- Verify audio recording starts/stops and saves.
- Verify all new memories appear in the main list.
