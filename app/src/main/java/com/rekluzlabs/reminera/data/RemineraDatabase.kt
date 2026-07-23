package com.rekluzlabs.reminera.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        MemoryEntryEntity::class,
        FamilyGroupEntity::class,
        FamilyMemberEntity::class,
        BiographyEntity::class,
        BiographySectionEntity::class,
        StoryEntryEntity::class
    ],
    version = 6,
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

    companion object {
        @Volatile
        private var INSTANCE: RemineraDatabase? = null

        fun getInstance(context: Context): RemineraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RemineraDatabase::class.java,
                    "reminera.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
