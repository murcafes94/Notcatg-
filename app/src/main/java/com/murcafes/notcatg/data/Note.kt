package com.murcafes.notcatg.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class Topic(
 @PrimaryKey(autoGenerate = true) val id: Long = 0,
 val name: String,
 val description: String = "",
 val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "resources")
data class Resource(
 @PrimaryKey(autoGenerate = true) val id: Long = 0,
 val topicId: Long,
 val type: String = "Pensamiento",
 val reference: String,
 val text: String = "",
 val source: String = "",
 val personalNote: String = "",
 val favorite: Boolean = false,
 val createdAt: Long = System.currentTimeMillis(),
 val updatedAt: Long = System.currentTimeMillis(),
 @ColumnInfo(defaultValue = "''") val importance: String = ""
)
