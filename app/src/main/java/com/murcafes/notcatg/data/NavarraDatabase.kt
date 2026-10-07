package com.murcafes.notcatg.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "verses", primaryKeys = ["book", "chapter", "verse"])
data class NavarraVerse(val book: String, val chapter: Int, val verse: String, val text: String, val printedLabel: String = verse)

@Dao
interface NavarraDao {
    @Query("SELECT COUNT(*) FROM verses") fun count(): Flow<Int>
    @Query("SELECT * FROM verses WHERE book = :book AND chapter = :chapter AND verse IN (:numbers)")
    suspend fun find(book: String, chapter: Int, numbers: List<String>): List<NavarraVerse>
    @Insert suspend fun insert(verses: List<NavarraVerse>)
    @Query("DELETE FROM verses") suspend fun clear()
    @Transaction suspend fun replaceBible(verses: List<NavarraVerse>) {
        clear()
        // Bound insertion batches; the whole replacement remains one transaction.
        verses.chunked(500).forEach { insert(it) }
    }
}

@Database(entities = [NavarraVerse::class], version = 1, exportSchema = false)
abstract class NavarraDatabase : RoomDatabase() {
    abstract fun verses(): NavarraDao
    companion object {
        @Volatile private var instance: NavarraDatabase? = null
        fun get(context: Context): NavarraDatabase = instance ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, NavarraDatabase::class.java, "navarra.db")
                .build().also { instance = it }
        }
    }
}

fun BibleReference.navarraNumbers(): List<String> = if (suffix.isNotEmpty()) listOf("$first$suffix") else (first..last).map { it.toString() }

fun BibleReference.navarraText(rows: List<NavarraVerse>): String? {
    val numbers = navarraNumbers()
    if (rows.size != numbers.size || rows.any { it.book != epubBook || it.chapter != chapter || it.text.isBlank() } || rows.map { it.verse }.toSet() != numbers.toSet()) return null
    val ordered = numbers.map { number -> rows.single { it.verse == number } }
    return ordered.distinctBy { it.printedLabel }.joinToString("\n") { verse ->
        if (numbers.size == 1 && verse.printedLabel == verse.verse) verse.text else "${verse.printedLabel}. ${verse.text}"
    }
}
