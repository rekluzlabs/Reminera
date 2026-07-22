package com.rekluzlabs.reminera.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [MemoryEntryEntity::class, FamilyGroupEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class RemineraDatabase : RoomDatabase() {

    abstract fun memoryEntryDao(): MemoryEntryDao
    abstract fun familyGroupDao(): FamilyGroupDao

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
                    // Intentionally used for pre-release / alpha development.
                    // Must be replaced with a real Migration before public release.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
