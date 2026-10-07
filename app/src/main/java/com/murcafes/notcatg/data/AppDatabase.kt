package com.murcafes.notcatg.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities=[Topic::class, Resource::class], version=2, exportSchema=false)
abstract class AppDatabase: RoomDatabase() {
 abstract fun noteDao(): NoteDao
 companion object {
  @Volatile private var INSTANCE: AppDatabase? = null
  fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
   Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "notcatg.db")
    .build().also { INSTANCE=it }
  }
 }
}
