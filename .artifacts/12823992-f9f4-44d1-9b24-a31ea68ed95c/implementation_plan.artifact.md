# Implementation Plan - Video Thumbnails

Support video thumbnails in the "Photos & Media" page and Home screen by generating them automatically from recorded or imported videos.

## User Review Required

> [!IMPORTANT]
> This change involves a database migration (Version 10 to 11) for the `story_entries` table to store the thumbnail URI. Existing videos will show a placeholder until a thumbnail is generated or they are re-added (though I will try to implement a lazy-loading fallback in the UI).

## Proposed Changes

### 1. Data Model & Database

#### [MODIFY] [StoryEntryEntity.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/data/StoryEntryEntity.kt)
- Add `val thumbnailUri: String? = null` field.

#### [MODIFY] [StoryEntryDao.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/data/StoryEntryDao.kt)
- Add `@Query("UPDATE story_entries SET thumbnailUri = :thumbnailUri WHERE id = :id") suspend fun updateThumbnailUri(id: String, thumbnailUri: String?)`.

#### [MODIFY] [RemineraDatabase.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/data/RemineraDatabase.kt)
- Increment version to 11.
- Add `MIGRATION_10_11` to add the `thumbnailUri` column to the `story_entries` table.

---

### 2. Utilities

#### [NEW] [ThumbnailHelper.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/util/ThumbnailHelper.kt)
- Create a utility class with a function `generateVideoThumbnail(context: Context, videoUri: Uri): String?` that:
    - Extracts a frame from the video using `MediaMetadataRetriever`.
    - Saves it as a JPEG file in the internal storage (`context.filesDir/thumbnails`).
    - Returns the absolute path to the thumbnail file.

---

### 3. ViewModels

#### [MODIFY] [RemineraViewModel.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/home/RemineraViewModel.kt)
- Update `addRecordedMemory` and `addImportedPhoto` to call `ThumbnailHelper.generateVideoThumbnail` when the type is `VIDEO` and store the result in `thumbnailPath`.
- Update `deleteEntry` to also delete the thumbnail file if it exists.

#### [MODIFY] [BiographyViewModel.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/biography/BiographyViewModel.kt)
- Update `addStoryEntry` to call `ThumbnailHelper.generateVideoThumbnail` when the type is `video` and store the result in the new `thumbnailUri` field.
- Update `handleMediaAction` (Delete case) to delete the thumbnail file.

---

### 4. UI Components

#### [MODIFY] [RemineraHomeScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/home/RemineraHomeScreen.kt)
- In `MemoryEntryCard`, prioritize `entry.thumbnailPath`. If null, keep the existing lazy-loading logic but consider moving it to `ThumbnailHelper` for consistency.

#### [MODIFY] [BiographyScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/biography/BiographyScreen.kt)
- In `MediaEntryRow`, update the `video` case to load the thumbnail from `entry.thumbnailUri`.
- Implement a similar lazy-loading fallback (using `MediaMetadataRetriever`) if `thumbnailUri` is null, so existing videos get thumbnails too.

## Verification Plan

### Automated Tests
- N/A (UI and Database migration focus)

### Manual Verification
1. **Migration Check**: Open the app and ensure existing data is preserved.
2. **Video Recording**: Record a video from the Biography screen. Verify the thumbnail appears in the "Photos & Media" list.
3. **Video Import**: Import a video from the gallery. Verify the thumbnail appears.
4. **Home Screen Check**: Verify that videos added/imported also show thumbnails on the Home screen.
5. **Deletion**: Delete a video and verify (via logs or file explorer if possible) that the thumbnail file is removed from internal storage.
