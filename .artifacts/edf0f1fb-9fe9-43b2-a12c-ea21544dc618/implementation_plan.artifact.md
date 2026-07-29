# Implementation Plan - Optimize External Video Intent Flags

The goal is to improve the user experience when opening YouTube and other external video links from the `BiographyScreen`. We will use specific Android Intent flags to ensure that external videos are opened in an isolated task and are not kept in the history, allowing users to return to Reminera immediately upon pressing "back" and avoiding the "feed trap".

## User Review Required

> [!IMPORTANT]
> We are using `Intent.FLAG_ACTIVITY_NEW_DOCUMENT` and `Intent.FLAG_ACTIVITY_NO_HISTORY`.
> - `FLAG_ACTIVITY_NEW_DOCUMENT` opens the video in a separate task in the app switcher.
> - `FLAG_ACTIVITY_NO_HISTORY` ensures the external activity is finished as soon as the user navigates away from it.

## Proposed Changes

### UI Components

#### [MODIFY] [BiographyScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/biography/BiographyScreen.kt)

Update the `onMediaClick` handler for "video" entries to include the recommended Intent flags for YouTube and Vimeo links, as well as the fallback browser intent.

```kotlin
// Example of the change to be applied:
val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
}
```

## Verification Plan

### Manual Verification
1. Open a biography that contains a YouTube link.
2. Tap on the YouTube video entry.
3. Verify that the YouTube app (or browser) opens the video.
4. Press the "Back" button or gesture.
5. Verify that you are immediately returned to the `BiographyScreen` in Reminera.
6. Check the Android "Recent Apps" switcher to ensure the YouTube video appeared as a separate document/task and is gone after returning.
