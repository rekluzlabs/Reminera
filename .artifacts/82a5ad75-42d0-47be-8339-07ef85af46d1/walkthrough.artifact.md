# Walkthrough - Image Cropping for Family Member Photos

I have integrated the image editor into the family member photo selection and viewing flows, allowing you to crop and rotate photos.

## Changes Made

### 1. Integrated Image Editor in Sheets
- **[FamilyMemberSheets.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/home/FamilyMemberSheets.kt)**:
    - Updated `AddFamilyMemberSheet` and `EditFamilyMemberSheet` to show the image editor immediately after a photo is picked from the gallery.
    - The preview in the sheet now shows the cropped result.

### 2. Profile Screen Enhancements
- **[RemineraHomeScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/home/RemineraHomeScreen.kt)**:
    - Updated the quick photo change logic (via long-press on avatar or FAB menu) to open the image editor before saving the new photo.

### 3. Full-Screen Editing
- **[FullScreenPhotoViewer.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/home/FullScreenPhotoViewer.kt)**:
    - Added a **Crop** icon to the top-right corner of the full-screen viewer.
- **[FamilyMemberListScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/home/FamilyMemberListScreen.kt)**:
    - Implemented the logic to trigger the editor when the Crop button is clicked in the full-screen viewer.

## Verification Results

### Automated Tests
- Build successful: `gradle_build("app:assembleDebug")` passed.

### Manual Verification
1.  **Add/Edit Member**:
    - Pick a photo. The editor opens.
    - Crop the photo and tap the checkmark.
    - Observe the cropped photo in the sheet preview.
2.  **Profile Screen**:
    - Long-press the avatar. Pick a photo.
    - Crop and save. Observe the updated avatar.
3.  **Full-Screen Viewer**:
    - Tap an avatar to see it full screen.
    - Tap the **Crop** icon in the top right.
    - Crop the photo. Observe the avatar is updated after saving.
