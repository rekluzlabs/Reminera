package com.rekluzlabs.reminera.util

import android.content.Context
import android.net.Uri
import com.rekluzlabs.reminera.data.RemineraDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupRestoreHelper {

    sealed class ProgressState {
        data object Idle : ProgressState()
        data class Running(val message: String) : ProgressState()
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

    suspend fun createBackup(context: Context, destinationUri: Uri): Boolean {
        val database = RemineraDatabase.getInstance(context)

        return withContext(Dispatchers.IO) {
            try {
                val stagingDir = File(context.cacheDir, "backup_staging_${System.currentTimeMillis()}")
                stagingDir.mkdirs()

                try {
                    val allMediaFiles = mutableSetOf<String>()

                    val groups = database.familyGroupDao().getAllOrderedBySortOrderList()
                    val members = database.familyMemberDao().getAllMembersList()
                    val entries = database.memoryEntryDao().getAllEntriesList()
                    val biographies = database.biographyDao().getAllBiographiesList()
                    val sections = database.biographySectionDao().getAllSectionsList()
                    val stories = database.storyEntryDao().getAllStoriesList()
                    val chapters = database.chapterExportDao().getAllChaptersList()
                    val manifests = database.bookExportManifestDao().getAllManifestsList()

                    entries.forEach { entry ->
                        allMediaFiles.add(entry.localFilePath)
                        entry.thumbnailPath?.let { allMediaFiles.add(it) }
                        entry.secondaryMediaPath?.let { allMediaFiles.add(it) }
                    }
                    members.forEach { member ->
                        member.photoUri?.let { allMediaFiles.add(it) }
                    }
                    biographies.forEach { bio ->
                        bio.photoUri?.let { allMediaFiles.add(it) }
                    }
                    stories.forEach { story ->
                        story.mediaUri?.let { allMediaFiles.add(it) }
                        story.thumbnailUri?.let { allMediaFiles.add(it) }
                    }
                    chapters.forEach { chapter ->
                        chapter.renderedPdfPath?.let { allMediaFiles.add(it) }
                    }

                    val backupData = JSONObject().apply {
                        put("app_version", "1.0")
                        put("app_files_dir", context.filesDir.absolutePath)
                        put("backup_timestamp", System.currentTimeMillis())
                        put("database", serializeGroups(groups, entries, members, biographies, sections, stories, chapters, manifests))
                        put("media_files", JSONArray(allMediaFiles.filter { File(it).exists() }))
                    }

                    File(stagingDir, "database.json").writeText(backupData.toString(2))

                    val mediaStaging = File(stagingDir, "media")
                    mediaStaging.mkdirs()

                    var copied = 0
                    allMediaFiles.filter { File(it).exists() }.forEach { path ->
                        val srcFile = File(path)
                        val relPath = getRelativeMediaPath(context, path)
                        if (relPath != null) {
                            val destFile = File(mediaStaging, relPath)
                            destFile.parentFile?.mkdirs()
                            srcFile.copyTo(destFile, overwrite = true)
                            copied++
                        }
                    }

                    val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
                    val prefsStaging = File(stagingDir, "shared_prefs")
                    prefsStaging.mkdirs()
                    listOf("reminera_settings.xml", "reminera_ai_consent.xml", "tutorial_prefs.xml", "reminera_prefs.xml").forEach { name ->
                        val src = File(sharedPrefsDir, name)
                        if (src.exists()) {
                            src.copyTo(File(prefsStaging, name), overwrite = true)
                        }
                    }

                    val zipFile = File(stagingDir, "reminera_backup.zip")
                    zipDirectory(stagingDir, zipFile)

                    context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                        FileInputStream(zipFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    setLastBackupTime(context, System.currentTimeMillis())
                    true
                } finally {
                    stagingDir.deleteRecursively()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun restoreBackup(context: Context, sourceUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val stagingDir = File(context.cacheDir, "restore_staging_${System.currentTimeMillis()}")
                stagingDir.mkdirs()

                try {
                    context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                        ZipInputStream(inputStream).use { zipInput ->
                            var entry = zipInput.nextEntry
                            while (entry != null) {
                                if (!entry.isDirectory) {
                                    val outFile = File(stagingDir, entry.name)
                                    outFile.parentFile?.mkdirs()
                                    FileOutputStream(outFile).use { fos ->
                                        zipInput.copyTo(fos)
                                    }
                                }
                                entry = zipInput.nextEntry
                            }
                        }
                    }

                    val dbJsonFile = File(stagingDir, "database.json")
                    if (!dbJsonFile.exists()) return@withContext false

                    val backupData = JSONObject(dbJsonFile.readText())
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

                    val mediaStaging = File(stagingDir, "media")
                    if (mediaStaging.exists()) {
                        val filesDir = context.filesDir
                        copyMediaFiles(mediaStaging, filesDir)
                    }

                    val prefsStaging = File(stagingDir, "shared_prefs")
                    if (prefsStaging.exists()) {
                        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
                        prefsDir.mkdirs()
                        prefsStaging.listFiles()?.forEach { file ->
                            file.copyTo(File(prefsDir, file.name), overwrite = true)
                        }
                    }

                    if (sourceFilesDir.isNotEmpty() && sourceFilesDir != targetFilesDir) {
                        remapFilePathsInDatabase(database, sourceFilesDir, targetFilesDir)
                    }

                    true
                } finally {
                    stagingDir.deleteRecursively()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
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
        groups: List<com.rekluzlabs.reminera.data.FamilyGroupEntity>,
        entries: List<com.rekluzlabs.reminera.data.MemoryEntryEntity>,
        members: List<com.rekluzlabs.reminera.data.FamilyMemberEntity>,
        biographies: List<com.rekluzlabs.reminera.data.BiographyEntity>,
        sections: List<com.rekluzlabs.reminera.data.BiographySectionEntity>,
        stories: List<com.rekluzlabs.reminera.data.StoryEntryEntity>,
        chapters: List<com.rekluzlabs.reminera.data.ChapterExportEntity>,
        manifests: List<com.rekluzlabs.reminera.data.BookExportManifestEntity>
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
                com.rekluzlabs.reminera.data.FamilyGroupEntity(
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
                com.rekluzlabs.reminera.data.FamilyMemberEntity(
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
                com.rekluzlabs.reminera.data.MemoryEntryEntity(
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
                com.rekluzlabs.reminera.data.BiographyEntity(
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
                com.rekluzlabs.reminera.data.BiographySectionEntity(
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
                com.rekluzlabs.reminera.data.StoryEntryEntity(
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
                com.rekluzlabs.reminera.data.ChapterExportEntity(
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
                com.rekluzlabs.reminera.data.BookExportManifestEntity(
                    id = obj.getLong("id"), groupId = obj.getLong("groupId"),
                    title = obj.getString("title"), memberOrderJson = obj.getString("memberOrderJson"),
                    dateCreated = obj.getLong("dateCreated"), lastModified = obj.getLong("lastModified")
                )
            )
        }
    }

    private fun copyMediaFiles(sourceDir: File, targetBaseDir: File) {
        sourceDir.listFiles()?.forEach { file ->
            val targetFile = File(targetBaseDir, file.name)
            if (file.isDirectory) {
                targetFile.mkdirs()
                copyMediaFiles(file, targetFile)
            } else {
                targetFile.parentFile?.mkdirs()
                file.copyTo(targetFile, overwrite = true)
            }
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

    private fun zipDirectory(sourceDir: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            sourceDir.walkTopDown().filter { it.isFile && it.name != "reminera_backup.zip" }.forEach { file ->
                val entryName = file.relativeTo(sourceDir).path
                zos.putNextEntry(ZipEntry(entryName))
                FileInputStream(file).use { fis ->
                    fis.copyTo(zos)
                }
                zos.closeEntry()
            }
        }
    }
}
