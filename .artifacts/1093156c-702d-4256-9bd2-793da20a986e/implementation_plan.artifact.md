# Implementation Plan - Secondary Media Support

This plan details the changes to support "secondary media" (audio or video attachments) for photo entries in the Reminera app. This allows photos to have an accompanying audio clip or video that plays in the full-screen viewer.

## Proposed Changes

### Data Layer

#### [MODIFY] [MemoryEntryEntity.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/data/MemoryEntryEntity.kt)
- Add `secondaryMediaPath: String? = null` and `secondaryMediaType: String? = null` fields to the `MemoryEntryEntity` data class.

#### [MODIFY] [RemineraDatabase.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/data/RemineraDatabase.kt)
- Increment the database version from `2` to `3` to trigger the destructive migration (as configured in the project) and update the schema.

---

### UI Layer

#### [MODIFY] [RemineraViewModel.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/home/RemineraViewModel.kt)
- Update `addImportedPhoto` to accept and store the new `secondaryMediaPath` and `secondaryMediaType` parameters.

#### [MODIFY] [FullScreenMediaViewer.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/detail/FullScreenMediaViewer.kt)
- Update `FullScreenMediaViewer` to support playing secondary media when viewing a photo.
- Add `FullScreenPhotoAudioChip` for audio playback overlay on photos.
- Update `FullScreenPhoto` to handle switching between the photo and a secondary video or displaying the audio chip.

## Verification Plan

### Automated Tests
- Run Gradle sync to ensure no compilation errors.
- Build the project to verify successful compilation with the new entity fields and UI changes.

### Manual Verification
- Verify that the app still builds and runs.
- Note: Since destructive migration is used, existing data will be lost upon the first run after the version bump.
