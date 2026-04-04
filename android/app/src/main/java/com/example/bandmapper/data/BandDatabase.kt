package com.example.bandmapper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BandData::class], version = 1, exportSchema = false)
abstract class BandDatabase : RoomDatabase() {
    abstract fun bandDataDao(): BandDataDao

    companion object {
        @Volatile
        private var INSTANCE: BandDatabase? = null

        fun getDatabase(context: Context): BandDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BandDatabase::class.java,
                    "band_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
