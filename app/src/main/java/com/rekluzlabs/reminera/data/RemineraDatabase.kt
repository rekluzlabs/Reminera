package com.rekluzlabs.reminera.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [MemoryEntryEntity::class, FamilyGroupEntity::class], version = 3, exportSchema = true)
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
                    // TODO: Replace with explicit Migration objects before
                    // public release (Room currently drops all user data on
                    // schema change). See app/schemas/ for exported schemas.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
