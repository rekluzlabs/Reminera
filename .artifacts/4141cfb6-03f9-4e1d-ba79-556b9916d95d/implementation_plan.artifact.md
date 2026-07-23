# Fix Deprecated Icons and SwipeToDismissBoxState

This plan addresses several deprecation warnings in the project related to Material Design icons and the `SwipeToDismissBoxState` API.

## User Review Required

> [!IMPORTANT]
> The `confirmValueChange` parameter in `rememberSwipeToDismissBoxState` has been deprecated. I am replacing it with a `LaunchedEffect` that observes the `currentValue` of the state. When a swipe gesture is completed (reaching `StartToEnd` or `EndToStart`), the corresponding action will be triggered, and the component will immediately "snap back" to the settled state. This maintains the existing "swipe-to-trigger-action" behavior.

## Proposed Changes

### UI Components

#### [MODIFY] [BiographyScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/biography/BiographyScreen.kt)
- Update `Icons.Default.MenuBook` to `Icons.AutoMirrored.Filled.MenuBook`.
- Replace deprecated `rememberSwipeToDismissBoxState` (with `confirmValueChange`) with the non-deprecated version and a `LaunchedEffect` to handle swipe actions.

#### [MODIFY] [StoryEntryScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/biography/StoryEntryScreen.kt)
- Update `Icons.Default.MenuBook` to `Icons.AutoMirrored.Filled.MenuBook`.

#### [MODIFY] [MemoryDetailScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/detail/MemoryDetailScreen.kt)
- Update `Icons.Default.DriveFileMove` to `Icons.AutoMirrored.Filled.DriveFileMove`.

#### [MODIFY] [ImageEditorScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/editor/ImageEditorScreen.kt)
- Update `Icons.Default.RotateRight` to `Icons.AutoMirrored.Filled.RotateRight`.

## Verification Plan

### Automated Tests
- Run `:app:assembleDebug` to verify that the deprecation warnings are gone (or at least these specific ones).

### Manual Verification
- Deploy the app and navigate to the Biography screen.
- Verify that swiping media entries still triggers the delete/download actions and snaps back correctly.
- Verify that the "Menu Book", "Drive File Move", and "Rotate Right" icons are still displayed correctly.
