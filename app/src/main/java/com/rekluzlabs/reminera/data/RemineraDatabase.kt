package com.rekluzlabs.reminera.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MemoryEntryEntity::class,
        FamilyGroupEntity::class,
        FamilyMemberEntity::class,
        BiographyEntity::class,
        BiographySectionEntity::class,
        StoryEntryEntity::class,
        ChapterExportEntity::class,
        BookExportManifestEntity::class
    ],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RemineraDatabase : RoomDatabase() {

    abstract fun memoryEntryDao(): MemoryEntryDao
    abstract fun familyGroupDao(): FamilyGroupDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun biographyDao(): BiographyDao
    abstract fun biographySectionDao(): BiographySectionDao
    abstract fun storyEntryDao(): StoryEntryDao
    abstract fun chapterExportDao(): ChapterExportDao
    abstract fun bookExportManifestDao(): BookExportManifestDao

    companion object {
        @Volatile
        private var INSTANCE: RemineraDatabase? = null

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE memory_entries ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    UPDATE memory_entries
                    SET sortOrder = (
                        SELECT COUNT(*)
                        FROM memory_entries AS m2
                        WHERE m2.groupId = memory_entries.groupId
                          AND m2.personTag = memory_entries.personTag
                          AND m2.dateCaptured < memory_entries.dateCaptured
                    )
                    """
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chapter_exports` (
                        `memberId` INTEGER NOT NULL,
                        `groupId` INTEGER NOT NULL,
                        `sourceDataHash` TEXT NOT NULL,
                        `generatedBioText` TEXT NOT NULL,
                        `mediaManifestJson` TEXT NOT NULL,
                        `lastGenerated` INTEGER NOT NULL,
                        PRIMARY KEY(`memberId`)
                    )
                    """
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `book_export_manifests` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `groupId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `memberOrderJson` TEXT NOT NULL,
                        `dateCreated` INTEGER NOT NULL,
                        `lastModified` INTEGER NOT NULL
                    )
                    """
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chapter_exports ADD COLUMN renderedPdfPath TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE chapter_exports ADD COLUMN renderedPdfHash TEXT DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): RemineraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RemineraDatabase::class.java,
                    "reminera.db"
                )
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
