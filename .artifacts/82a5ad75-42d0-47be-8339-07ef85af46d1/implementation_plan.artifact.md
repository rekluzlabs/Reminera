# Implementation Plan - Image Cropping for Family Member Photos

This plan adds image cropping and rotation capabilities when selecting or changing a family member's photo by integrating the existing `ImageEditorScreen`.

## User Review Required

> [!IMPORTANT]
> When you pick a new photo for a family member, the app will now open an editor where you can crop and rotate the image before it is saved. This will apply to both the "Add/Edit Member" sheets and the quick photo change on the profile screen.

## Proposed Changes

### [UI Components]

#### [MODIFY] [FamilyMemberSheets.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/home/FamilyMemberSheets.kt)
- Add `editingPhotoUri` state to both `AddFamilyMemberSheet` and `EditFamilyMemberSheet`.
- When a photo is picked from the gallery, instead of just updating the preview, set `editingPhotoUri`.
- If `editingPhotoUri` is not null, display the `ImageEditorScreen` over the sheet content.
- On save in the editor, update the `photoUri` state with the edited image's URI and clear `editingPhotoUri`.

#### [MODIFY] [RemineraHomeScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/home/RemineraHomeScreen.kt)
- Add `editingPhotoUri` state.
- When a photo is picked (via tap/long tap or FAB menu), set `editingPhotoUri`.
- If `editingPhotoUri` is not null, display the `ImageEditorScreen`.
- On save in the editor, copy the edited image to internal storage and call `viewModel.updateMemberPhoto`.

#### [MODIFY] [FullScreenPhotoViewer.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/home/FullScreenPhotoViewer.kt)
- Add an "Edit" button to the full-screen viewer.
- Clicking the Edit button will trigger the `ImageEditorScreen` for the currently viewed photo.

## Verification Plan

### Manual Verification
- Go to any family group and tap "Add family member".
- Pick a photo and verify the editor opens. Crop the photo and save.
- Verify the cropped preview appears in the sheet.
- Go to a member's profile and tap their photo.
- In the full-screen viewer, tap the new "Edit" icon (if added) or just use the long-press shortcut to pick a new photo and crop it.
- Verify that the final avatar in the profile and member list is correctly cropped.
