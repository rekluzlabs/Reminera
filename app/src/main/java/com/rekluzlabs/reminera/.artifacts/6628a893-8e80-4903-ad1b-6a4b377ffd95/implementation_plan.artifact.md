# Move Themes to a Separate Section in Settings

The goal is to clean up the main settings page by moving the theme selection options into a dedicated "Themes" sub-section.

## Proposed Changes

### [Component Name] UI Settings

#### [NEW] [ThemeSettingsScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/settings/ThemeSettingsScreen.kt)
- Created a new screen dedicated to theme selection.
- It displays the list of available themes using `ThemeOption`.

#### [MODIFY] [SettingsScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/settings/SettingsScreen.kt)
- Removed the inline theme selection section.
- Added a "SETTINGS" category with a "Themes" entry that triggers navigation to the theme sub-screen.
- Added a `onNavigateToThemes` callback to the `SettingsScreen` signature.

#### [MODIFY] [MainActivity.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/MainActivity.kt)
- Introduced a `SettingsSection` enum or simple state to track the current settings view (Main vs Themes).
- Updated the `showSettings` block in `setContent` to handle navigation between the main settings screen and the theme settings screen.

## Verification Plan

### Manual Verification
1. Open Settings from any screen.
2. Verify the "Themes" option is visible under a "SETTINGS" section.
3. Tap "Themes" and verify it navigates to the dedicated Theme Settings screen.
4. Verify theme selection still works and updates the app theme immediately.
5. Verify the back button in Theme Settings returns to the main Settings screen.
6. Verify the back button in main Settings returns to the previous screen.
