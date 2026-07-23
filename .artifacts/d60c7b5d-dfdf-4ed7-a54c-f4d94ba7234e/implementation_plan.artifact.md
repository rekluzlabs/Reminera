# Implementation Plan - Fix Image Editor Save Functionality

Resolve the issue where changes (like cropping) are not being saved correctly, and improve the user experience for saving edited photos.

## User Review Required

> [!IMPORTANT]
> The "Save" button will now automatically apply any active crop selection before saving. This means you no longer *have* to click the checkmark before clicking Save.

## Proposed Changes

### ViewModel

#### [MODIFY] [ImageEditorScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/editor/ImageEditorScreen.kt) (ImageEditorViewModel)
- Simplify `saveImage` to save directly to the app's cache directory instead of MediaStore. Since the biography screen immediately copies the result to its own internal storage, saving to the public gallery first is redundant and potentially prone to permission issues.
- Ensure `viewModelScope.launch` is used for loading and saving operations to avoid blocking the UI.

### UI Layer

#### [MODIFY] [ImageEditorScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/editor/ImageEditorScreen.kt) (ImageEditorScreen)
- Update the "Save" `IconButton` logic:
    - If `showCropOverlay` is true, call `viewModel.applyCrop(normalizedCropRect)` before proceeding to save.
    - This ensures that if a user adjusts the crop box and hits "Save" directly (forgetting the checkmark), their crop is still applied.

#### [MODIFY] [BiographyScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/biography/BiographyScreen.kt)
- Add a small delay or loading indicator if needed (optional, current logic is async which is good).
- Ensure `fullScreenPhotoUri` is cleared and reset correctly after save to force a UI refresh of the viewer.

## Verification Plan

### Automated Tests
- Build successful.

### Manual Verification
1. **Crop & Save (with checkmark)**:
    - Tap Crop, adjust box, tap checkmark.
    - Tap Save.
    - Verify image updates in biography.
2. **Crop & Save (WITHOUT checkmark)**:
    - Tap Crop, adjust box.
    - Tap Save directly.
    - Verify the crop is correctly applied and saved.
3. **Rotate & Save**:
    - Tap Rotate, then tap Save.
    - Verify orientation persists.
