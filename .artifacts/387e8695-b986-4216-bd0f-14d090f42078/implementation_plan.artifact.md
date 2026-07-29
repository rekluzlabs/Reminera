# [REVISED] Fix YouTube Direct Launch Issue

This plan implements the optimized YouTube direct launch using the `vnd.youtube:` URI scheme. This ensures the YouTube app opens directly to the video, avoiding the home feed transition.

## User Review Required

> [!IMPORTANT]
> - Following your feedback, we are skipping the `<queries>` manifest entry and redundant Intent flags.
> - We will use the `vnd.youtube:$videoId` scheme with `setPackage("com.google.android.youtube")` for maximum reliability.

## Proposed Changes

### UI Layer

#### [MODIFY] [BiographyScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/biography/BiographyScreen.kt)
Update the `onMediaClick` handler for "video" entries:
- Use `ThumbnailHelper.extractYouTubeVideoId(uri)` to get the video ID.
- Create an Intent with `vnd.youtube:$videoId` and set package to `com.google.android.youtube`.
- Fall back to a standard `https://` web Intent if the YouTube app is not installed.
- Remove redundant flags like `FLAG_ACTIVITY_NEW_TASK` or `FLAG_ACTIVITY_NEW_DOCUMENT` unless testing shows they are required.

## Verification Plan

### Automated Tests
- N/A (UI Interaction)

### Manual Verification
1. **Cold Start**: Open a YouTube video when the YouTube app is closed. Verify it opens directly to the video.
2. **Warm Start**: Open a YouTube video when the YouTube app is already in the background. Verify it switches to the video directly.
3. **No App Fallback**: (Optional/Simulator) Verify that if the YouTube app is not present, it opens in the system browser.

> [!NOTE]
> On some devices, a brief transition may still be visible as the YouTube app initializes; this is behavior internal to the YouTube app.
