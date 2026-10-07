package com.murcafes.notcatg.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
 @Query("SELECT * FROM topics ORDER BY name COLLATE NOCASE")
 fun topics(): Flow<List<Topic>>

 @Query("""SELECT * FROM topics WHERE name LIKE '%' || :q || '%' OR description LIKE '%' || :q || '%' ORDER BY name COLLATE NOCASE""")
 fun searchTopics(q: String): Flow<List<Topic>>

 @Query("SELECT * FROM resources WHERE topicId = :topicId ORDER BY favorite DESC, updatedAt DESC")
 fun resources(topicId: Long): Flow<List<Resource>>

 @Query("""SELECT * FROM resources WHERE reference LIKE '%' || :q || '%' OR text LIKE '%' || :q || '%' OR source LIKE '%' || :q || '%' OR personalNote LIKE '%' || :q || '%' OR type LIKE '%' || :q || '%' ORDER BY updatedAt DESC""")
 fun searchResources(q: String): Flow<List<Resource>>

 @Insert suspend fun insertTopic(topic: Topic): Long
 @Update suspend fun updateTopic(topic: Topic)
 @Delete suspend fun deleteTopic(topic: Topic)

 @Insert suspend fun insertResource(resource: Resource): Long
 @Update suspend fun updateResource(resource: Resource)
 @Delete suspend fun deleteResource(resource: Resource)

 @Query("DELETE FROM resources WHERE topicId = :topicId")
 suspend fun deleteResourcesForTopic(topicId: Long)
}
