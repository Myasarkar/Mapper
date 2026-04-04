package com.example.bandmapper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BandDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bandData: BandData): Long

    @Query("SELECT * FROM band_data ORDER BY timestamp DESC")
    fun getAllBandData(): Flow<List<BandData>>

    @Query("DELETE FROM band_data")
    suspend fun deleteAll()
}
