# Reminera — Project Context

> Preserve your family's history, stories, and voices — completely offline and under your control.
> Copyright (c) 2026 Rekluz Labs. All rights reserved.

---

## Project Overview

Reminera is an offline-first Android application for documenting, organizing, and safeguarding the memories, personal histories, and voices of family members — especially older relatives — before their stories are lost to time.

**Status:** Early Alpha — core capture/import flows working, HTML chapter template + WebView-to-PDF rendering built (Step 2 of 5 for PDF/QR export).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + StateFlow (manual `ViewModelProvider.Factory`, no DI framework) |
| Database | Room (version 8, schema exported) |
| Camera | CameraX |
| Audio/Video Playback | Media3 ExoPlayer (`PlaybackManager` wrapper) |
| Navigation | Navigation Compose (15 routes) |
| Reorder | `sh.calvin.reorderable` library |
| Build | Single-module `:app`, Gradle Kotlin DSL |
| Package | `com.rekluzlabs.reminera` |

---

## Core Concept / Planning

- [x] App concept finalized (offline-first family memory-recording app)
- [x] Core feature list locked (audio/video recording, old photo import, book-style PDF export with QR codes)
- [ ] **"Online-hosted audio/video" piece scoped** — `hostedUrl` and `uploadStatus` fields exist on `MemoryEntryEntity`, but no hosting solution is chosen or implemented. This is a hard dependency for PDF/QR export.
- [x] Target platform — Android, `minSdk 28`, `targetSdk 36`, `compileSdk 37`

---

## Architecture Foundation

- [x] Project scaffolded (single-module `:app`, Compose, no XML layouts)
- [x] MVVM + StateFlow pattern established
- [x] Manual `ViewModelProvider.Factory` pattern in place (no Hilt/Dagger/Koin)
- [x] Navigation Compose routing set up (15 routes in `MainActivity`)
- [x] Material 3 theming implemented with `ThemeManager`
- [x] All 7 theme modes built (Light, Dark, AMOLED × 3 variants, plus system-default modes)
- [x] Room database set up (`RemineraDatabase`, version 8, schema export enabled)
- [x] All 8 entities defined and modeled (6 original + 2 export entities)
- [ ] Relationships between entities — handled in repository/ViewModel layer via queries, **no Room `@Relation` annotations**. Worth formalizing if query complexity grows.

### Entities

| Entity | Table | Key Fields |
|---|---|---|
| `MemoryEntryEntity` | `memory_entries` | id (String UUID), groupId, title, type (PHOTO/VIDEO/AUDIO), localFilePath, thumbnailPath, personTag, notes, dateCaptured, dateAdded, durationMillis, isImported, uploadStatus, hostedUrl, secondaryMediaPath, secondaryMediaType, sortOrder |
| `FamilyGroupEntity` | `family_groups` | id (Long auto), name, groupType, sortOrder, createdAt |
| `FamilyMemberEntity` | `family_members` | id (Long auto), groupId (FK→family_groups), name, role, biography, birthDate, photoUri, sortOrder, createdAt |
| `BiographyEntity` | `biographies` | id (String), personId, fullName, relationship, birthDate, familyGroupId, photoUri, createdAt, updatedAt |
| `BiographySectionEntity` | `biography_sections` | id (String), biographyId, sectionType, fieldsJson, updatedAt |
| `StoryEntryEntity` | `story_entries` | id (String), biographyId, contributedBy, type, mediaUri, textContent, recordedAt, createdAt |
| `ChapterExportEntity` | `chapter_exports` | memberId (PK), groupId, sourceDataHash, generatedBioText, mediaManifestJson, lastGenerated |
| `BookExportManifestEntity` | `book_export_manifests` | id (Long auto), groupId, title, memberOrderJson, dateCreated, lastModified |

### DAOs

| DAO | Key Queries |
|---|---|
| `MemoryEntryDao` | getAllEntries, getEntriesByGroupIdAndPersonTag(List), insert, update, delete, updateSortOrders, updatePersonTag |
| `FamilyGroupDao` | getAllOrderedBySortOrder, getEntryCounts, getMemberCounts, insert, update, deleteByIds (transaction) |
| `FamilyMemberDao` | getMembersByGroupId(List), getMemberById, insert, update, deleteById |
| `BiographyDao` | getByPersonId(Flow/suspend), getById, insert, update, deleteById |
| `BiographySectionDao` | getByBiographyId(Flow/suspend), getByBiographyIdAndType, insert, update |
| `StoryEntryDao` | getByBiographyId(Flow/suspend), insert, deleteById, updateMediaUri, updateTextContent, getById |
| `ChapterExportDao` | getByMemberId, upsert, deleteByMemberId |
| `BookExportManifestDao` | getById, getByGroupId, insert, update, deleteById |

### Navigation Routes (15)

`splash_onboarding`, `family_groups`, `family_members/{groupId}`, `reminera_home/{groupId}`, `memory_detail/{memoryId}`, `edit_memory/{memoryId}`, `media_viewer/{memoryId}`, `video_record`, `audio_record`, `biography/{memberId}`, `story_entries/{memberId}`, `settings`, `theme_settings`, `family_group_picker/{memoryId}`

---

## Family Members Feature

- [x] Family Members page/screen built
- [x] `LazyVerticalGrid` for family group tiles implemented (`FamilyGroupsScreen`)
- [x] Add/edit/remove family member flow (bottom sheets + dialogs)
- [x] Navigation from family member → their photos/media page (filtered `RemineraHomeScreen`)

---

## Photos and Media Page

- [x] Media list UI built — **`LazyColumn`** (not grid), items grouped by month/year with section headers
- [x] Audio playback working (ExoPlayer via `PlaybackManager`, inline on cards + full-screen)
- [x] Video playback working (ExoPlayer/Media3 `PlayerView`, full-screen with transport controls)
- [x] Media loading/display confirmed stable
- [x] Old family photo import flow built (Photo Picker + internal storage copy)
- [x] Photo import from device gallery/storage working (`PickVisualMedia.ImageOnly`)
- [x] Video import from device gallery working (`PickVisualMedia.VideoOnly`)
- [x] Audio import from device working (`OpenDocument` with `audio/*`)
- [x] Camera capture — photo (`TakePicture`) and video (`CaptureVideo`) both implemented
- [x] Full-screen media viewer (`FullScreenMediaViewer`) with photo, audio, and video views

---

## Media Item Actions

- [x] 3-dot overflow menu UI added per item (`MediaItemMenuSheet` modal bottom sheet)
- [x] Rename action implemented (dialog + file rename on disk)
- [x] Move action implemented — reassign to different family member (`MoveToMemberDialog`)
- [x] Move to different group also implemented (`MoveMemoryDialog`)
- [x] Download action implemented (`MediaStore` / scoped storage via `DownloadHelper`)
- [x] Save to device action implemented (saves to `DCIM/Reminera` via `MediaSaver`)
- [x] Delete action implemented with confirmation dialog (removes DB entry + file + thumbnail)
- [x] Long-press drag reorder implemented (`sh.calvin.reorderable` library)
- [x] `sortOrder` field added to `MemoryEntryEntity` + Room migration (`MIGRATION_6_7`)
- [x] Reorder mode / overflow menu gesture conflict resolved (menus hidden during reorder)
- [ ] **Multi-select / batch delete** — explicitly deferred. Not started. Requires separate "Select" toolbar action.

---

## Memory Entries

- [x] Memory entry recording flow (audio) — standalone screen + inline in add-memory sheet
- [x] Memory entry recording flow (video) — standalone screen with CameraX
- [x] Memory entry list UI (`LazyColumn` with month/year grouping)
- [x] Memory entry metadata (date, personTag, notes/labels, duration, import status)
- [ ] **Narratives & Captions** — `notes` field exists but rich text story editor not built

---

## Biography / Story Feature

- [x] Biography entity + sections entity defined
- [x] Biography screen (`BiographyScreen`) with sections
- [x] Story entry entity + screen (`StoryEntriesScreen`)
- [x] Add story entry dialog with media type selection

---

## PDF / QR Export

- [x] **Chapter export caching data layer** (Step 1 of 5):
  - [x] `ChapterExportEntity` — caches generated chapter text + source data hash, keyed by memberId
  - [x] `BookExportManifestEntity` — stores ordered list of memberIds per book/group
  - [x] `ChapterHasher` — pure SHA-256 hash of member profile + bio sections + stories + media IDs
  - [x] `ChapterExportRepository.getOrGenerateChapter()` — cache-check with placeholder text generation
  - [x] `BookExportManifestRepository` — add/remove/reorder members without touching cached chapters
  - [x] `MIGRATION_7_8` — creates `chapter_exports` and `book_export_manifests` tables
  - [x] Unit tests for hash stability + repository cache behavior + manifest isolation
- [x] **HTML chapter template + WebView-to-PDF rendering** (Step 2 of 5):
  - [x] `ChapterHtmlTemplateBuilder` — self-contained HTML with inline CSS variables, base64 images, photo grids, QR placeholder blocks for VIDEO/AUDIO, paragraph splitting
  - [x] `ChapterPdfRenderer` — WebView headless rendering via `WebView.draw(Canvas)` + `PdfDocument` (avoids `PrintDocumentAdapter` callback package-private constructor issue on compileSdk 37)
  - [x] Debug-only trigger in `FamilyMemberListScreen` (`BuildConfig.DEBUG` gated, FileProvider URI)
  - [x] `ChapterHtmlTemplateBuilderTest` — 15 tests covering HTML structure, CSS variables, photo/video/audio rendering, QR placeholders, self-contained validation
- [ ] BYOK AI biography generation — not started (Step 3, replaces placeholder text)
- [ ] QR code generation + hosting — not started (Step 4)
- [ ] Full book assembly + multi-chapter PDF merge — not started (Step 5)
- [ ] PDF export tested on a real device/printer

---

## Data & Storage

- [x] Local file storage strategy finalized — recordings in `context.filesDir/recordings/`, imported media copied to internal storage via `FileUtils.copyUriToInternal()`
- [x] Room migration strategy — `MIGRATION_6_7` for sortOrder, `MIGRATION_7_8` for export tables; earlier versions use `fallbackToDestructiveMigration(dropAllTables = true)`
- [ ] **Backup/restore or data export** — not implemented. Settings screen is minimal (theme selector only).

---

## Testing

- [x] Test infrastructure established:
  - [x] `room-testing` dependency added (v2.8.0, version-matched)
  - [x] `kotlinx-coroutines-test` dependency added (v1.10.2)
  - [x] `robolectric` dependency added (v4.14.1, pinned to SDK 35)
  - [x] `androidx-test-core` dependency added (v1.6.1)
  - [x] In-memory Room DB test pattern established
- [x] `ChapterHasherTest` — 8 tests covering hash stability, field sensitivity, null handling, SHA-256 format
- [x] `ChapterExportRepositoryTest` — 5 tests covering cache hit, bio edit invalidation, media reorder invalidation, manifest JSON, placeholder text
- [x] `BookExportManifestRepositoryTest` — 6 tests covering manifest isolation, create/get, add/remove, reorder, duplicate prevention
- [x] `ChapterHtmlTemplateBuilderTest` — 15 tests covering HTML structure, CSS variables, photo tiles, media placeholders, QR blocks, self-contained validation (uses Robolectric + temp JPEG files)
- [ ] Core flows manually tested end-to-end (no integration tests yet)
- [ ] Testing on multiple API levels
- [ ] Testing on multiple theme modes
- [ ] UI tests beyond boilerplate

---

## Settings & UI Polish

- [x] Settings screen exists (theme selector + branding)
- [x] Tutorial system built (custom coordinator pattern with overlay)
- [x] Splash/onboarding screen
- [ ] No account/profile settings
- [ ] No notification settings
- [ ] No privacy/security settings
- [ ] No data management/export settings in UI

---

## Identified Gaps / Open Questions

1. **Hosting solution for PDF/QR export** — The `hostedUrl` field exists on `MemoryEntryEntity` and `UploadStatus` enum exists, but there is zero implementation or decision on where audio/video files will be hosted. This blocks Steps 4-5 of PDF/QR export. Options to evaluate: Firebase Storage, Supabase, self-hosted, or user-provided cloud (BYOK as README suggests).

2. **Entity relationships are not formalized in Room** — Queries join data in repositories/ViewModels manually. Consider `@Relation` annotations if join complexity grows.

3. **No data backup/restore** — All data lives in internal storage with no export path. Loss of app data = loss of everything. This should be prioritized before any public release.

4. **`fallbackToDestructiveMigration` is active** — Only migrations `6→7` and `7→8` are explicit. Any schema change on versions 1–6 drops all data. Acceptable for alpha, but must be fixed before users have real data.

5. **Placeholder text in chapter exports** — `ChapterExportRepository.getOrGenerateChapter()` generates placeholder biography text marked with `TODO(step-4)`. This is intentional for Step 1; real AI generation comes in Step 4.

6. **Multi-select / batch operations** — Explicitly deferred but worth tracking as a known UX gap.

7. **Rich text story editor** — `notes` field is plain string. The "Narratives & Captions" feature in the README vision is not implemented.

8. **Settings is bare** — Only theme + branding. Missing: backup/restore, about, privacy info, version details, storage usage.

9. **No `AGENTS.md`** — No agent instructions file for AI-assisted development continuity.
