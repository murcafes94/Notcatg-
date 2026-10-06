package com.murcafes.notcatg.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
 @Query("""SELECT * FROM notes
 WHERE (:q = '' OR title LIKE '%' || :q || '%' OR content LIKE '%' || :q || '%' OR category LIKE '%' || :q || '%' OR tags LIKE '%' || :q || '%')
 ORDER BY favorite DESC, updatedAt DESC""")
 fun search(q: String): Flow<List<Note>>

 @Insert suspend fun insert(note: Note): Long
 @Update suspend fun update(note: Note)
 @Delete suspend fun delete(note: Note)
}
