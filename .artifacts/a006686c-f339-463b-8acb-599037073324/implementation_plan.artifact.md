# Implementation Plan - Inline Playback and Full Screen Toggle

This plan enables inline video playback in the detail screen preview and adds a dedicated full-screen toggle button.

## User Review Required

> [!IMPORTANT]
> I will remove the `clickable` modifier from the entire video card and instead provide two distinct interactive areas: a Play/Pause button and a Full Screen button. This ensures that tapping the preview doesn't accidentally trigger the wrong action.

## Proposed Changes

### [app]

#### [MODIFY] [MemoryDetailScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/example/reminera/ui/detail/MemoryDetailScreen.kt)
- **Update `ViewMediaPreview`**:
    - Remove the `clickable` modifier from the `Card` when the type is `VIDEO`.
    - Pass the `onClick` (full screen action) down to `ViewVideoPreview`.
- **Update `ViewVideoPreview`**:
    - Add local `isPlaying` state.
    - Reposition the Play/Pause icon to toggle inline playback using the `VideoView`.
    - Add a `Fullscreen` icon in the bottom-left corner that triggers the full-screen viewer.
    - Ensure `shouldPause` still works by pausing the `VideoView` and updating the `isPlaying` state when the full-screen mode is opened.
- **Refine `ViewAudioPreview`**:
    - Ensure consistency in UI if needed, though the request specifically mentioned video.

## Verification Plan

### Automated Tests
- Build the project using `./gradlew :app:assembleDebug`.

### Manual Verification
- Open a video memory:
    - Tap the play icon in the bottom right: verify the video plays/pauses in the small window.
    - Tap the full-screen icon in the bottom left: verify the full-screen viewer opens.
    - While playing inline, open full screen: verify the inline video pauses.
