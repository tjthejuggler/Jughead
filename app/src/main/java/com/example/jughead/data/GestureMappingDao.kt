package com.example.jughead.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GestureMappingDao {
    @Query("SELECT * FROM gesture_mapping")
    fun getAllMappings(): Flow<List<GestureMapping>>

    @Query("SELECT * FROM gesture_mapping WHERE gestureName = :gestureName LIMIT 1")
    suspend fun getMappingByGesture(gestureName: String): GestureMapping?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: GestureMapping): Long

    @Delete
    suspend fun deleteMapping(mapping: GestureMapping): Int
}
