package com.murcafes.notcatg.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities=[Topic::class, Resource::class], version=3, exportSchema=false)
abstract class AppDatabase: RoomDatabase() {
 abstract fun noteDao(): NoteDao
 companion object {
  val MIGRATION_2_3 = object : Migration(2, 3) {
   override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE resources ADD COLUMN importance TEXT NOT NULL DEFAULT ''")
   }
  }
  @Volatile private var INSTANCE: AppDatabase? = null
  fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
   Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "notcatg.db")
    .addMigrations(MIGRATION_2_3)
    .build().also { INSTANCE=it }
  }
 }
}
