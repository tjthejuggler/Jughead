package com.example.jughead.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gesture_mapping")
data class GestureMapping(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    
    @ColumnInfo(name = "gestureName")
    val gestureName: String,
    
    @ColumnInfo(name = "commandName")
    val commandName: String
)
