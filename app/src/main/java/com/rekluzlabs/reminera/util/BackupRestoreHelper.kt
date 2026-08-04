package com.rekluzlabs.reminera.util

import android.content.Context
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import com.rekluzlabs.reminera.data.BiographyEntity
import com.rekluzlabs.reminera.data.BiographySectionEntity
import com.rekluzlabs.reminera.data.BookExportManifestEntity
import com.rekluzlabs.reminera.data.ChapterExportEntity
import com.rekluzlabs.reminera.data.FamilyGroupEntity
import com.rekluzlabs.reminera.data.FamilyMemberEntity
import com.rekluzlabs.reminera.data.MemoryEntryEntity
import com.rekluzlabs.reminera.data.RemineraDatabase
import com.rekluzlabs.reminera.data.StoryEntryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupRestoreHelper {

    sealed class ProgressState {
        data object Idle : ProgressState()
        data class Running(
            val message: String,
            val progress: Float = -1f, // -1 means indeterminate
            val detail: String? = null
        ) : ProgressState()
        data object Success : ProgressState()
        data class Error(val message: String) : ProgressState()
    }

    private const val PREFS_NAME = "reminera_backup_prefs"
    private const val KEY_LAST_BACKUP_TIME = "last_backup_time"

    fun getLastBackupTime(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_BACKUP_TIME, 0L)
    }

    private fun setLastBackupTime(context: Context, time: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_BACKUP_TIME, time).apply()
    }

    private const val TAG = "BackupRestoreHelper"

    suspend fun createBackup(
        context: Context,
        destinationUri: Uri,
        onProgress: (ProgressState) -> Unit = {}
    ): Boolean {
        val database = RemineraDatabase.getInstance(context)
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Reminera::BackupWakeLock")

        return withContext(Dispatchers.IO) {
            try {
                wakeLock.acquire(15 * 60 * 1000L) // 15 min timeout
                onProgress(ProgressState.Running("Preparing backup data...", 0.05f))

                val groups = database.familyGroupDao().getAllOrderedBySortOrderList()
                val members = database.familyMemberDao().getAllMembersList()
                val entries = database.memoryEntryDao().getAllEntriesList()
                val biographies = database.biographyDao().getAllBiographiesList()
                val sections = database.biographySectionDao().getAllSectionsList()
                val stories = database.storyEntryDao().getAllStoriesList()
                val chapters = database.chapterExportDao().getAllChaptersList()
                val manifests = database.bookExportManifestDao().getAllManifestsList()

                val allMediaFiles = mutableSetOf<String>()
                entries.forEach { entry ->
                    allMediaFiles.add(entry.localFilePath)
                    entry.thumbnailPath?.let { allMediaFiles.add(it) }
                    entry.secondaryMediaPath?.let { allMediaFiles.add(it) }
                }
                members.forEach { member -> member.photoUri?.let { allMediaFiles.add(it) } }
                biographies.forEach { bio -> bio.photoUri?.let { allMediaFiles.add(it) } }
                stories.forEach { story ->
                    story.mediaUri?.let { allMediaFiles.add(it) }
                    story.thumbnailUri?.let { allMediaFiles.add(it) }
                }
                chapters.forEach { chapter -> chapter.renderedPdfPath?.let { allMediaFiles.add(it) } }

                val existingMediaFiles = allMediaFiles.filter { File(it).exists() }
                
                val backupData = JSONObject().apply {
                    put("app_version", "1.0")
                    put("app_files_dir", context.filesDir.absolutePath)
                    put("backup_timestamp", System.currentTimeMillis())
                    put("database", serializeGroups(groups, entries, members, biographies, sections, stories, chapters, manifests))
                    put("media_files", JSONArray(existingMediaFiles))
                }

                context.contentResolver.openOutputStream(destinationUri)?.use { rawOutput ->
                    ZipOutputStream(BufferedOutputStream(rawOutput, 65536)).use { zos ->
                        // 1. Add database.json
                        onProgress(ProgressState.Running("Zipping database...", 0.1f))
                        zos.putNextEntry(ZipEntry("database.json"))
                        zos.write(backupData.toString(2).toByteArray())
                        zos.closeEntry()

                        // 2. Add media files
                        val totalFiles = existingMediaFiles.size
                        existingMediaFiles.forEachIndexed { index, path ->
                            val srcFile = File(path)
                            val relPath = getRelativeMediaPath(context, path)
                            if (relPath != null) {
                                val progress = 0.1f + (index.toFloat() / totalFiles) * 0.8f
                                onProgress(ProgressState.Running("Zipping media: ${srcFile.name}", progress, "${index + 1}/$totalFiles"))
                                
                                val isCompressed = srcFile.extension.lowercase() in listOf("jpg", "jpeg", "png", "mp4", "m4a", "wav", "mp3", "pdf")
                                zos.setLevel(if (isCompressed) Deflater.NO_COMPRESSION else Deflater.BEST_COMPRESSION)
                                
                                try {
                                    zos.putNextEntry(ZipEntry("media/$relPath"))
                                    FileInputStream(srcFile).use { fis ->
                                        fis.copyTo(zos)
                                    }
                                    zos.closeEntry()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to zip file: $path", e)
                                }
                            }
                        }

                        // 3. Add shared prefs
                        onProgress(ProgressState.Running("Zipping settings...", 0.95f))
                        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
                        listOf("reminera_settings.xml", "reminera_ai_consent.xml", "tutorial_prefs.xml", "reminera_prefs.xml").forEach { name ->
                            val src = File(sharedPrefsDir, name)
                            if (src.exists()) {
                                zos.putNextEntry(ZipEntry("shared_prefs/$name"))
                                FileInputStream(src).use { it.copyTo(zos) }
                                zos.closeEntry()
                            }
                        }
                    }
                }

                setLastBackupTime(context, System.currentTimeMillis())
                onProgress(ProgressState.Success)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Backup failed", e)
                onProgress(ProgressState.Error("Backup failed: ${e.localizedMessage}"))
                false
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
            }
        }
    }

    suspend fun restoreBackup(
        context: Context,
        sourceUri: Uri,
        onProgress: (ProgressState) -> Unit = {}
    ): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Reminera::RestoreWakeLock")

        return withContext(Dispatchers.IO) {
            try {
                wakeLock.acquire(15 * 60 * 1000L)
                onProgress(ProgressState.Running("Opening backup file...", 0.05f))

                val totalSize = try {
                    context.contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                            if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L
                        } else 0L
                    } ?: 0L
                } catch (_: Exception) { 0L }

                var bytesReadTotal = 0L
                val tempDbFile = File(context.cacheDir, "temp_db_${System.currentTimeMillis()}.json")

                context.contentResolver.openInputStream(sourceUri)?.use { rawInput ->
                    val trackingInput = object : java.io.FilterInputStream(rawInput) {
                        override fun read(): Int = super.read().also { if (it != -1) bytesReadTotal++ }
                        override fun read(b: ByteArray, off: Int, len: Int): Int =
                            super.read(b, off, len).also { if (it != -1) bytesReadTotal += it.toLong() }
                    }

                    ZipInputStream(trackingInput).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                val progress = if (totalSize > 0) 0.05f + (bytesReadTotal.toFloat() / totalSize) * 0.75f else -1f
                                onProgress(ProgressState.Running("Extracting: ${entry.name}", progress.coerceIn(0f, 0.8f)))

                                val targetFile = when {
                                    entry.name == "database.json" -> tempDbFile
                                    entry.name.startsWith("media/") -> {
                                        val relPath = entry.name.removePrefix("media/")
                                        File(context.filesDir, relPath)
                                    }
                                    entry.name.startsWith("shared_prefs/") -> {
                                        val relPath = entry.name.removePrefix("shared_prefs/")
                                        File(context.applicationInfo.dataDir, "shared_prefs/$relPath")
                                    }
                                    else -> null
                                }

                                if (targetFile != null) {
                                    targetFile.parentFile?.mkdirs()
                                    FileOutputStream(targetFile).use { fos ->
                                        zis.copyTo(fos)
                                    }
                                }
                            }
                            entry = zis.nextEntry
                        }
                    }
                }

                if (!tempDbFile.exists()) {
                    onProgress(ProgressState.Error("Invalid backup: database.json missing"))
                    return@withContext false
                }

                onProgress(ProgressState.Running("Restoring database records...", 0.85f))
                val backupData = JSONObject(tempDbFile.readText())
                val dbData = backupData.getJSONObject("database")
                val database = RemineraDatabase.getInstance(context)

                database.clearAllTables()

                val sourceFilesDir = backupData.optString("app_files_dir", "")
                val targetFilesDir = context.filesDir.absolutePath

                restoreGroups(database, dbData.getJSONArray("family_groups"))
                restoreMembers(database, dbData.getJSONArray("family_members"))
                restoreEntries(database, dbData.getJSONArray("memory_entries"))
                restoreBiographies(database, dbData.getJSONArray("biographies"))
                restoreSections(database, dbData.getJSONArray("biography_sections"))
                restoreStories(database, dbData.getJSONArray("story_entries"))
                restoreChapters(database, dbData.getJSONArray("chapter_exports"))
                restoreManifests(database, dbData.getJSONArray("book_export_manifests"))

                if (sourceFilesDir.isNotEmpty() && sourceFilesDir != targetFilesDir) {
                    onProgress(ProgressState.Running("Remapping file paths...", 0.95f))
                    remapFilePathsInDatabase(database, sourceFilesDir, targetFilesDir)
                }

                tempDbFile.delete()
                onProgress(ProgressState.Success)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed", e)
                onProgress(ProgressState.Error("Restore failed: ${e.localizedMessage}"))
                false
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
            }
        }
    }

    private suspend fun remapFilePathsInDatabase(
        database: RemineraDatabase,
        sourceDir: String,
        targetDir: String
    ) {
        database.memoryEntryDao().getAllEntriesList().forEach { entry ->
            var changed = false
            val newLocal = if (entry.localFilePath.startsWith(sourceDir)) {
                changed = true; entry.localFilePath.replace(sourceDir, targetDir)
            } else entry.localFilePath
            val newThumb = entry.thumbnailPath?.let {
                if (it.startsWith(sourceDir)) { changed = true; it.replace(sourceDir, targetDir) } else it
            }
            val newSec = entry.secondaryMediaPath?.let {
                if (it.startsWith(sourceDir)) { changed = true; it.replace(sourceDir, targetDir) } else it
            }
            if (changed) {
                database.memoryEntryDao().insertDirect(entry.copy(
                    localFilePath = newLocal,
                    thumbnailPath = newThumb,
                    secondaryMediaPath = newSec
                ))
            }
        }

        database.familyMemberDao().getAllMembersList().forEach { member ->
            member.photoUri?.let { uri ->
                if (uri.startsWith(sourceDir)) {
                    database.familyMemberDao().insertDirect(member.copy(
                        photoUri = uri.replace(sourceDir, targetDir)
                    ))
                }
            }
        }

        database.biographyDao().getAllBiographiesList().forEach { bio ->
            bio.photoUri?.let { uri ->
                if (uri.startsWith(sourceDir)) {
                    database.biographyDao().insertDirect(bio.copy(
                        photoUri = uri.replace(sourceDir, targetDir)
                    ))
                }
            }
        }

        database.storyEntryDao().getAllStoriesList().forEach { story ->
            var changed = false
            val newMedia = story.mediaUri?.let {
                if (it.startsWith(sourceDir)) { changed = true; it.replace(sourceDir, targetDir) } else it
            }
            val newThumb = story.thumbnailUri?.let {
                if (it.startsWith(sourceDir)) { changed = true; it.replace(sourceDir, targetDir) } else it
            }
            if (changed) {
                database.storyEntryDao().insertDirect(story.copy(
                    mediaUri = newMedia,
                    thumbnailUri = newThumb
                ))
            }
        }

        database.chapterExportDao().getAllChaptersList().forEach { chapter ->
            chapter.renderedPdfPath?.let { path ->
                if (path.startsWith(sourceDir)) {
                    database.chapterExportDao().upsert(chapter.copy(
                        renderedPdfPath = path.replace(sourceDir, targetDir)
                    ))
                }
            }
        }
    }

    suspend fun createMemberBackup(
        context: Context,
        memberId: Long,
        destinationUri: Uri,
        onProgress: (ProgressState) -> Unit = {}
    ): Boolean {
        val database = RemineraDatabase.getInstance(context)
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Reminera::MemberBackupWakeLock")

        return withContext(Dispatchers.IO) {
            try {
                wakeLock.acquire(10 * 60 * 1000L)
                onProgress(ProgressState.Running("Gathering member data...", 0.1f))

                val member = database.familyMemberDao().getMemberById(memberId)
                    ?: return@withContext false
                val group = database.familyGroupDao().getAllOrderedBySortOrderList()
                    .find { it.id == member.groupId }
                val biography = database.biographyDao().getByPersonIdOnce(memberId)
                val sections = biography?.let {
                    database.biographySectionDao().getByBiographyIdOnce(it.id)
                } ?: emptyList()
                val stories = biography?.let {
                    database.storyEntryDao().getByBiographyIdOnce(it.id)
                } ?: emptyList()
                val memoryEntries = database.memoryEntryDao().getEntriesByGroupIdAndPersonTagList(member.groupId, member.name)
                val chapter = database.chapterExportDao().getByMemberId(memberId)

                val allMediaFiles = mutableSetOf<String>()
                memoryEntries.forEach { entry ->
                    allMediaFiles.add(entry.localFilePath)
                    entry.thumbnailPath?.let { allMediaFiles.add(it) }
                    entry.secondaryMediaPath?.let { allMediaFiles.add(it) }
                }
                member.photoUri?.let { allMediaFiles.add(it) }
                biography?.photoUri?.let { allMediaFiles.add(it) }
                stories.forEach { story ->
                    story.mediaUri?.let { allMediaFiles.add(it) }
                    story.thumbnailUri?.let { allMediaFiles.add(it) }
                }
                chapter?.renderedPdfPath?.let { allMediaFiles.add(it) }

                val existingMediaFiles = allMediaFiles.filter { File(it).exists() }

                val backupData = JSONObject().apply {
                    put("app_version", "1.0")
                    put("app_files_dir", context.filesDir.absolutePath)
                    put("backup_timestamp", System.currentTimeMillis())
                    put("backup_type", "member")
                    put("member_id", memberId)
                    put("database", JSONObject().apply {
                        put("family_groups", JSONArray().also { arr ->
                            group?.let { g ->
                                arr.put(JSONObject().apply {
                                    put("id", g.id); put("name", g.name); put("groupType", g.groupType)
                                    put("sortOrder", g.sortOrder); put("createdAt", g.createdAt)
                                })
                            }
                        })
                        put("family_members", JSONArray().also { arr ->
                            arr.put(JSONObject().apply {
                                put("id", member.id); put("groupId", member.groupId); put("name", member.name)
                                put("role", member.role); put("biography", member.biography)
                                put("birthDate", member.birthDate ?: JSONObject.NULL)
                                put("photoUri", member.photoUri ?: JSONObject.NULL)
                                put("sortOrder", member.sortOrder); put("createdAt", member.createdAt)
                            })
                        })
                        put("memory_entries", JSONArray().also { arr ->
                            memoryEntries.forEach { e -> arr.put(JSONObject().apply {
                                put("id", e.id); put("groupId", e.groupId); put("title", e.title)
                                put("type", e.type); put("localFilePath", e.localFilePath)
                                put("thumbnailPath", e.thumbnailPath ?: JSONObject.NULL)
                                put("personTag", e.personTag ?: JSONObject.NULL)
                                put("notes", e.notes ?: JSONObject.NULL)
                                put("dateCaptured", e.dateCaptured); put("dateAdded", e.dateAdded)
                                put("durationMillis", e.durationMillis ?: JSONObject.NULL)
                                put("isImported", e.isImported); put("uploadStatus", e.uploadStatus)
                                put("hostedUrl", e.hostedUrl ?: JSONObject.NULL)
                                put("secondaryMediaPath", e.secondaryMediaPath ?: JSONObject.NULL)
                                put("secondaryMediaType", e.secondaryMediaType ?: JSONObject.NULL)
                                put("sortOrder", e.sortOrder)
                            }) }
                        })
                        put("biographies", JSONArray().also { arr ->
                            biography?.let { b ->
                                arr.put(JSONObject().apply {
                                    put("id", b.id); put("personId", b.personId); put("fullName", b.fullName)
                                    put("relationship", b.relationship)
                                    put("birthDate", b.birthDate ?: JSONObject.NULL)
                                    put("familyGroupId", b.familyGroupId)
                                    put("photoUri", b.photoUri ?: JSONObject.NULL)
                                    put("createdAt", b.createdAt); put("updatedAt", b.updatedAt)
                                })
                            }
                        })
                        put("biography_sections", JSONArray().also { arr ->
                            sections.forEach { s -> arr.put(JSONObject().apply {
                                put("id", s.id); put("biographyId", s.biographyId)
                                put("sectionType", s.sectionType); put("fieldsJson", s.fieldsJson)
                                put("updatedAt", s.updatedAt)
                            }) }
                        })
                        put("story_entries", JSONArray().also { arr ->
                            stories.forEach { s -> arr.put(JSONObject().apply {
                                put("id", s.id); put("biographyId", s.biographyId)
                                put("contributedBy", s.contributedBy); put("type", s.type)
                                put("mediaUri", s.mediaUri ?: JSONObject.NULL)
                                put("thumbnailUri", s.thumbnailUri ?: JSONObject.NULL)
                                put("textContent", s.textContent ?: JSONObject.NULL)
                                put("recordedAt", s.recordedAt); put("createdAt", s.createdAt)
                            }) }
                        })
                        put("chapter_exports", JSONArray().also { arr ->
                            chapter?.let { c ->
                                arr.put(JSONObject().apply {
                                    put("memberId", c.memberId); put("groupId", c.groupId)
                                    put("sourceDataHash", c.sourceDataHash); put("generatedBioText", c.generatedBioText)
                                    put("mediaManifestJson", c.mediaManifestJson); put("lastGenerated", c.lastGenerated)
                                    put("renderedPdfPath", c.renderedPdfPath ?: JSONObject.NULL)
                                    put("renderedPdfHash", c.renderedPdfHash ?: JSONObject.NULL)
                                    put("biographySource", c.biographySource)
                                    put("aiPolishedAt", c.aiPolishedAt ?: JSONObject.NULL)
                                })
                            }
                        })
                        put("book_export_manifests", JSONArray())
                    })
                    put("media_files", JSONArray(existingMediaFiles))
                }

                context.contentResolver.openOutputStream(destinationUri)?.use { rawOutput ->
                    ZipOutputStream(BufferedOutputStream(rawOutput, 65536)).use { zos ->
                        onProgress(ProgressState.Running("Zipping member database...", 0.2f))
                        zos.putNextEntry(ZipEntry("database.json"))
                        zos.write(backupData.toString(2).toByteArray())
                        zos.closeEntry()

                        val totalFiles = existingMediaFiles.size
                        existingMediaFiles.forEachIndexed { index, path ->
                            val srcFile = File(path)
                            val relPath = getRelativeMediaPath(context, path)
                            if (relPath != null) {
                                val progress = 0.2f + (index.toFloat() / totalFiles) * 0.8f
                                onProgress(ProgressState.Running("Zipping media: ${srcFile.name}", progress, "${index + 1}/$totalFiles"))

                                val isCompressed = srcFile.extension.lowercase() in listOf("jpg", "jpeg", "png", "mp4", "m4a", "wav", "mp3", "pdf")
                                zos.setLevel(if (isCompressed) Deflater.NO_COMPRESSION else Deflater.BEST_COMPRESSION)

                                try {
                                    zos.putNextEntry(ZipEntry("media/$relPath"))
                                    FileInputStream(srcFile).use { fis ->
                                        fis.copyTo(zos)
                                    }
                                    zos.closeEntry()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to zip file: $path", e)
                                }
                            }
                        }
                    }
                }

                onProgress(ProgressState.Success)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Member backup failed", e)
                onProgress(ProgressState.Error("Backup failed: ${e.localizedMessage}"))
                false
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
            }
        }
    }

    suspend fun restoreMemberBackup(
        context: Context,
        sourceUri: Uri,
        onProgress: (ProgressState) -> Unit = {}
    ): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Reminera::RestoreWakeLock")

        return withContext(Dispatchers.IO) {
            try {
                wakeLock.acquire(10 * 60 * 1000L)
                onProgress(ProgressState.Running("Opening backup file...", 0.05f))

                val totalSize = try {
                    context.contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                            if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L
                        } else 0L
                    } ?: 0L
                } catch (_: Exception) { 0L }

                var bytesReadTotal = 0L
                val tempDbFile = File(context.cacheDir, "temp_db_member_${System.currentTimeMillis()}.json")

                context.contentResolver.openInputStream(sourceUri)?.use { rawInput ->
                    val trackingInput = object : java.io.FilterInputStream(rawInput) {
                        override fun read(): Int = super.read().also { if (it != -1) bytesReadTotal++ }
                        override fun read(b: ByteArray, off: Int, len: Int): Int =
                            super.read(b, off, len).also { if (it != -1) bytesReadTotal += it.toLong() }
                    }

                    ZipInputStream(trackingInput).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                val progress = if (totalSize > 0) 0.05f + (bytesReadTotal.toFloat() / totalSize) * 0.75f else -1f
                                onProgress(ProgressState.Running("Extracting: ${entry.name}", progress.coerceIn(0f, 0.8f)))

                                val targetFile = when {
                                    entry.name == "database.json" -> tempDbFile
                                    entry.name.startsWith("media/") -> {
                                        val relPath = entry.name.removePrefix("media/")
                                        File(context.filesDir, relPath)
                                    }
                                    else -> null
                                }

                                if (targetFile != null) {
                                    targetFile.parentFile?.mkdirs()
                                    FileOutputStream(targetFile).use { fos ->
                                        zis.copyTo(fos)
                                    }
                                }
                            }
                            entry = zis.nextEntry
                        }
                    }
                }

                if (!tempDbFile.exists()) {
                    onProgress(ProgressState.Error("Invalid backup: database.json missing"))
                    return@withContext false
                }

                onProgress(ProgressState.Running("Restoring member records...", 0.85f))
                val backupData = JSONObject(tempDbFile.readText())
                val dbData = backupData.getJSONObject("database")
                val database = RemineraDatabase.getInstance(context)

                val sourceFilesDir = backupData.optString("app_files_dir", "")
                val targetFilesDir = context.filesDir.absolutePath

                val groupsArr = dbData.optJSONArray("family_groups") ?: JSONArray()
                for (i in 0 until groupsArr.length()) {
                    val obj = groupsArr.getJSONObject(i)
                    val existing = database.familyGroupDao().getAllOrderedBySortOrderList()
                        .find { it.id == obj.getLong("id") }
                    if (existing == null) {
                        database.familyGroupDao().insertDirect(
                            FamilyGroupEntity(
                                id = obj.getLong("id"), name = obj.getString("name"),
                                groupType = obj.getString("groupType"), sortOrder = obj.getInt("sortOrder"),
                                createdAt = obj.getLong("createdAt")
                            )
                        )
                    }
                }

                val membersArr = dbData.optJSONArray("family_members") ?: JSONArray()
                for (i in 0 until membersArr.length()) {
                    val obj = membersArr.getJSONObject(i)
                    database.familyMemberDao().insertDirect(
                        FamilyMemberEntity(
                            id = obj.getLong("id"), groupId = obj.getLong("groupId"),
                            name = obj.getString("name"), role = obj.getString("role"),
                            biography = obj.optString("biography", ""),
                            birthDate = if (obj.isNull("birthDate")) null else obj.getLong("birthDate"),
                            photoUri = if (obj.isNull("photoUri")) null else obj.getString("photoUri"),
                            sortOrder = obj.optInt("sortOrder", 0),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }

                val biosArr = dbData.optJSONArray("biographies") ?: JSONArray()
                for (i in 0 until biosArr.length()) {
                    val obj = biosArr.getJSONObject(i)
                    database.biographyDao().insertDirect(
                        BiographyEntity(
                            id = obj.getString("id"), personId = obj.getLong("personId"),
                            fullName = obj.getString("fullName"), relationship = obj.getString("relationship"),
                            birthDate = if (obj.isNull("birthDate")) null else obj.getLong("birthDate"),
                            familyGroupId = obj.getLong("familyGroupId"),
                            photoUri = if (obj.isNull("photoUri")) null else obj.getString("photoUri"),
                            createdAt = obj.getLong("createdAt"), updatedAt = obj.getLong("updatedAt")
                        )
                    )
                }

                val sectionsArr = dbData.optJSONArray("biography_sections") ?: JSONArray()
                for (i in 0 until sectionsArr.length()) {
                    val obj = sectionsArr.getJSONObject(i)
                    database.biographySectionDao().insertDirect(
                        BiographySectionEntity(
                            id = obj.getString("id"), biographyId = obj.getString("biographyId"),
                            sectionType = obj.getString("sectionType"), fieldsJson = obj.getString("fieldsJson"),
                            updatedAt = obj.getLong("updatedAt")
                        )
                    )
                }

                val storiesArr = dbData.optJSONArray("story_entries") ?: JSONArray()
                for (i in 0 until storiesArr.length()) {
                    val obj = storiesArr.getJSONObject(i)
                    database.storyEntryDao().insertDirect(
                        StoryEntryEntity(
                            id = obj.getString("id"), biographyId = obj.getString("biographyId"),
                            contributedBy = obj.getString("contributedBy"), type = obj.getString("type"),
                            mediaUri = if (obj.isNull("mediaUri")) null else obj.getString("mediaUri"),
                            thumbnailUri = if (obj.isNull("thumbnailUri")) null else obj.getString("thumbnailUri"),
                            textContent = if (obj.isNull("textContent")) null else obj.getString("textContent"),
                            recordedAt = obj.getLong("recordedAt"), createdAt = obj.getLong("createdAt")
                        )
                    )
                }

                val entriesArr = dbData.optJSONArray("memory_entries") ?: JSONArray()
                for (i in 0 until entriesArr.length()) {
                    val obj = entriesArr.getJSONObject(i)
                    database.memoryEntryDao().insertDirect(
                        MemoryEntryEntity(
                            id = obj.getString("id"), groupId = obj.optLong("groupId", 0L),
                            title = obj.getString("title"), type = obj.getString("type"),
                            localFilePath = obj.getString("localFilePath"),
                            thumbnailPath = if (obj.isNull("thumbnailPath")) null else obj.getString("thumbnailPath"),
                            personTag = if (obj.isNull("personTag")) null else obj.getString("personTag"),
                            notes = if (obj.isNull("notes")) null else obj.getString("notes"),
                            dateCaptured = obj.getLong("dateCaptured"), dateAdded = obj.getLong("dateAdded"),
                            durationMillis = if (obj.isNull("durationMillis")) null else obj.getLong("durationMillis"),
                            isImported = obj.getBoolean("isImported"),
                            uploadStatus = obj.optString("uploadStatus", "NOT_UPLOADED"),
                            hostedUrl = if (obj.isNull("hostedUrl")) null else obj.getString("hostedUrl"),
                            secondaryMediaPath = if (obj.isNull("secondaryMediaPath")) null else obj.getString("secondaryMediaPath"),
                            secondaryMediaType = if (obj.isNull("secondaryMediaType")) null else obj.getString("secondaryMediaType"),
                            sortOrder = obj.optInt("sortOrder", 0)
                        )
                    )
                }

                val chaptersArr = dbData.optJSONArray("chapter_exports") ?: JSONArray()
                for (i in 0 until chaptersArr.length()) {
                    val obj = chaptersArr.getJSONObject(i)
                    database.chapterExportDao().upsert(
                        ChapterExportEntity(
                            memberId = obj.getLong("memberId"), groupId = obj.getLong("groupId"),
                            sourceDataHash = obj.getString("sourceDataHash"),
                            generatedBioText = obj.getString("generatedBioText"),
                            mediaManifestJson = obj.getString("mediaManifestJson"),
                            lastGenerated = obj.getLong("lastGenerated"),
                            renderedPdfPath = if (obj.isNull("renderedPdfPath")) null else obj.getString("renderedPdfPath"),
                            renderedPdfHash = if (obj.isNull("renderedPdfHash")) null else obj.getString("renderedPdfHash"),
                            biographySource = obj.optString("biographySource", "RAW"),
                            aiPolishedAt = if (obj.isNull("aiPolishedAt")) null else obj.getLong("aiPolishedAt")
                        )
                    )
                }

                if (sourceFilesDir.isNotEmpty() && sourceFilesDir != targetFilesDir) {
                    onProgress(ProgressState.Running("Remapping file paths...", 0.95f))
                    remapFilePathsInDatabase(database, sourceFilesDir, targetFilesDir)
                }

                tempDbFile.delete()
                onProgress(ProgressState.Success)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed", e)
                onProgress(ProgressState.Error("Restore failed: ${e.localizedMessage}"))
                false
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
            }
        }
    }

    suspend fun clearAllData(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val database = RemineraDatabase.getInstance(context)
                database.clearAllTables()

                val filesDir = context.filesDir
                listOf("media", "recordings", "exports", "thumbnails").forEach { dirName ->
                    val dir = File(filesDir, dirName)
                    if (dir.exists()) {
                        dir.deleteRecursively()
                    }
                }

                listOf(
                    "reminera_settings",
                    "reminera_ai_consent",
                    "reminera_backup_prefs",
                    "reminera_prefs",
                    "tutorial_prefs",
                    "reminera_ai_prefs"
                ).forEach { prefsName ->
                    context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                        .edit().clear().apply()
                    context.deleteSharedPreferences(prefsName)
                }

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun serializeGroups(
        groups: List<FamilyGroupEntity>,
        entries: List<MemoryEntryEntity>,
        members: List<FamilyMemberEntity>,
        biographies: List<BiographyEntity>,
        sections: List<BiographySectionEntity>,
        stories: List<StoryEntryEntity>,
        chapters: List<ChapterExportEntity>,
        manifests: List<BookExportManifestEntity>
    ): JSONObject {
        return JSONObject().apply {
            put("family_groups", JSONArray().also { arr ->
                groups.forEach { g -> arr.put(JSONObject().apply {
                    put("id", g.id); put("name", g.name); put("groupType", g.groupType)
                    put("sortOrder", g.sortOrder); put("createdAt", g.createdAt)
                }) }
            })
            put("family_members", JSONArray().also { arr ->
                members.forEach { m -> arr.put(JSONObject().apply {
                    put("id", m.id); put("groupId", m.groupId); put("name", m.name)
                    put("role", m.role); put("biography", m.biography)
                    put("birthDate", m.birthDate ?: JSONObject.NULL)
                    put("photoUri", m.photoUri ?: JSONObject.NULL)
                    put("sortOrder", m.sortOrder); put("createdAt", m.createdAt)
                }) }
            })
            put("memory_entries", JSONArray().also { arr ->
                entries.forEach { e -> arr.put(JSONObject().apply {
                    put("id", e.id); put("groupId", e.groupId); put("title", e.title)
                    put("type", e.type); put("localFilePath", e.localFilePath)
                    put("thumbnailPath", e.thumbnailPath ?: JSONObject.NULL)
                    put("personTag", e.personTag ?: JSONObject.NULL)
                    put("notes", e.notes ?: JSONObject.NULL)
                    put("dateCaptured", e.dateCaptured); put("dateAdded", e.dateAdded)
                    put("durationMillis", e.durationMillis ?: JSONObject.NULL)
                    put("isImported", e.isImported); put("uploadStatus", e.uploadStatus)
                    put("hostedUrl", e.hostedUrl ?: JSONObject.NULL)
                    put("secondaryMediaPath", e.secondaryMediaPath ?: JSONObject.NULL)
                    put("secondaryMediaType", e.secondaryMediaType ?: JSONObject.NULL)
                    put("sortOrder", e.sortOrder)
                }) }
            })
            put("biographies", JSONArray().also { arr ->
                biographies.forEach { b -> arr.put(JSONObject().apply {
                    put("id", b.id); put("personId", b.personId); put("fullName", b.fullName)
                    put("relationship", b.relationship)
                    put("birthDate", b.birthDate ?: JSONObject.NULL)
                    put("familyGroupId", b.familyGroupId)
                    put("photoUri", b.photoUri ?: JSONObject.NULL)
                    put("createdAt", b.createdAt); put("updatedAt", b.updatedAt)
                }) }
            })
            put("biography_sections", JSONArray().also { arr ->
                sections.forEach { s -> arr.put(JSONObject().apply {
                    put("id", s.id); put("biographyId", s.biographyId)
                    put("sectionType", s.sectionType); put("fieldsJson", s.fieldsJson)
                    put("updatedAt", s.updatedAt)
                }) }
            })
            put("story_entries", JSONArray().also { arr ->
                stories.forEach { s -> arr.put(JSONObject().apply {
                    put("id", s.id); put("biographyId", s.biographyId)
                    put("contributedBy", s.contributedBy); put("type", s.type)
                    put("mediaUri", s.mediaUri ?: JSONObject.NULL)
                    put("thumbnailUri", s.thumbnailUri ?: JSONObject.NULL)
                    put("textContent", s.textContent ?: JSONObject.NULL)
                    put("recordedAt", s.recordedAt); put("createdAt", s.createdAt)
                }) }
            })
            put("chapter_exports", JSONArray().also { arr ->
                chapters.forEach { c -> arr.put(JSONObject().apply {
                    put("memberId", c.memberId); put("groupId", c.groupId)
                    put("sourceDataHash", c.sourceDataHash); put("generatedBioText", c.generatedBioText)
                    put("mediaManifestJson", c.mediaManifestJson); put("lastGenerated", c.lastGenerated)
                    put("renderedPdfPath", c.renderedPdfPath ?: JSONObject.NULL)
                    put("renderedPdfHash", c.renderedPdfHash ?: JSONObject.NULL)
                    put("biographySource", c.biographySource)
                    put("aiPolishedAt", c.aiPolishedAt ?: JSONObject.NULL)
                }) }
            })
            put("book_export_manifests", JSONArray().also { arr ->
                manifests.forEach { m -> arr.put(JSONObject().apply {
                    put("id", m.id); put("groupId", m.groupId); put("title", m.title)
                    put("memberOrderJson", m.memberOrderJson)
                    put("dateCreated", m.dateCreated); put("lastModified", m.lastModified)
                }) }
            })
        }
    }

    private suspend fun restoreGroups(database: RemineraDatabase, arr: JSONArray) {
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            database.familyGroupDao().insertDirect(
                FamilyGroupEntity(
                    id = obj.getLong("id"), name = obj.getString("name"),
                    groupType = obj.getString("groupType"), sortOrder = obj.getInt("sortOrder"),
                    createdAt = obj.getLong("createdAt")
                )
            )
        }
    }

    private suspend fun restoreMembers(database: RemineraDatabase, arr: JSONArray) {
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            database.familyMemberDao().insertDirect(
                FamilyMemberEntity(
                    id = obj.getLong("id"), groupId = obj.getLong("groupId"),
                    name = obj.getString("name"), role = obj.getString("role"),
                    biography = obj.optString("biography", ""),
                    birthDate = if (obj.isNull("birthDate")) null else obj.getLong("birthDate"),
                    photoUri = if (obj.isNull("photoUri")) null else obj.getString("photoUri"),
                    sortOrder = obj.optInt("sortOrder", 0),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
    }

    private suspend fun restoreEntries(database: RemineraDatabase, arr: JSONArray) {
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            database.memoryEntryDao().insertDirect(
                MemoryEntryEntity(
                    id = obj.getString("id"), groupId = obj.optLong("groupId", 0L),
                    title = obj.getString("title"), type = obj.getString("type"),
                    localFilePath = obj.getString("localFilePath"),
                    thumbnailPath = if (obj.isNull("thumbnailPath")) null else obj.getString("thumbnailPath"),
                    personTag = if (obj.isNull("personTag")) null else obj.getString("personTag"),
                    notes = if (obj.isNull("notes")) null else obj.getString("notes"),
                    dateCaptured = obj.getLong("dateCaptured"), dateAdded = obj.getLong("dateAdded"),
                    durationMillis = if (obj.isNull("durationMillis")) null else obj.getLong("durationMillis"),
                    isImported = obj.getBoolean("isImported"),
                    uploadStatus = obj.optString("uploadStatus", "NOT_UPLOADED"),
                    hostedUrl = if (obj.isNull("hostedUrl")) null else obj.getString("hostedUrl"),
                    secondaryMediaPath = if (obj.isNull("secondaryMediaPath")) null else obj.getString("secondaryMediaPath"),
                    secondaryMediaType = if (obj.isNull("secondaryMediaType")) null else obj.getString("secondaryMediaType"),
                    sortOrder = obj.optInt("sortOrder", 0)
                )
            )
        }
    }

    private suspend fun restoreBiographies(database: RemineraDatabase, arr: JSONArray) {
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            database.biographyDao().insertDirect(
                BiographyEntity(
                    id = obj.getString("id"), personId = obj.getLong("personId"),
                    fullName = obj.getString("fullName"), relationship = obj.getString("relationship"),
                    birthDate = if (obj.isNull("birthDate")) null else obj.getLong("birthDate"),
                    familyGroupId = obj.getLong("familyGroupId"),
                    photoUri = if (obj.isNull("photoUri")) null else obj.getString("photoUri"),
                    createdAt = obj.getLong("createdAt"), updatedAt = obj.getLong("updatedAt")
                )
            )
        }
    }

    private suspend fun restoreSections(database: RemineraDatabase, arr: JSONArray) {
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            database.biographySectionDao().insertDirect(
                BiographySectionEntity(
                    id = obj.getString("id"), biographyId = obj.getString("biographyId"),
                    sectionType = obj.getString("sectionType"), fieldsJson = obj.getString("fieldsJson"),
                    updatedAt = obj.getLong("updatedAt")
                )
            )
        }
    }

    private suspend fun restoreStories(database: RemineraDatabase, arr: JSONArray) {
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            database.storyEntryDao().insertDirect(
                StoryEntryEntity(
                    id = obj.getString("id"), biographyId = obj.getString("biographyId"),
                    contributedBy = obj.getString("contributedBy"), type = obj.getString("type"),
                    mediaUri = if (obj.isNull("mediaUri")) null else obj.getString("mediaUri"),
                    thumbnailUri = if (obj.isNull("thumbnailUri")) null else obj.getString("thumbnailUri"),
                    textContent = if (obj.isNull("textContent")) null else obj.getString("textContent"),
                    recordedAt = obj.getLong("recordedAt"), createdAt = obj.getLong("createdAt")
                )
            )
        }
    }

    private suspend fun restoreChapters(database: RemineraDatabase, arr: JSONArray) {
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            database.chapterExportDao().upsert(
                ChapterExportEntity(
                    memberId = obj.getLong("memberId"), groupId = obj.getLong("groupId"),
                    sourceDataHash = obj.getString("sourceDataHash"),
                    generatedBioText = obj.getString("generatedBioText"),
                    mediaManifestJson = obj.getString("mediaManifestJson"),
                    lastGenerated = obj.getLong("lastGenerated"),
                    renderedPdfPath = if (obj.isNull("renderedPdfPath")) null else obj.getString("renderedPdfPath"),
                    renderedPdfHash = if (obj.isNull("renderedPdfHash")) null else obj.getString("renderedPdfHash"),
                    biographySource = obj.optString("biographySource", "RAW"),
                    aiPolishedAt = if (obj.isNull("aiPolishedAt")) null else obj.getLong("aiPolishedAt")
                )
            )
        }
    }

    private suspend fun restoreManifests(database: RemineraDatabase, arr: JSONArray) {
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            database.bookExportManifestDao().insertDirect(
                BookExportManifestEntity(
                    id = obj.getLong("id"), groupId = obj.getLong("groupId"),
                    title = obj.getString("title"), memberOrderJson = obj.getString("memberOrderJson"),
                    dateCreated = obj.getLong("dateCreated"), lastModified = obj.getLong("lastModified")
                )
            )
        }
    }

    private fun getRelativeMediaPath(context: Context, absolutePath: String): String? {
        val baseDir = context.filesDir.absolutePath
        return if (absolutePath.startsWith(baseDir)) {
            absolutePath.removePrefix(baseDir).trimStart('/')
        } else {
            null
        }
    }
}
