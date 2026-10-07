package com.murcafes.notcatg.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
 @Query("SELECT * FROM topics ORDER BY id")
 suspend fun backupTopics(): List<Topic>

 @Query("SELECT * FROM resources ORDER BY id")
 suspend fun backupResources(): List<Resource>

 @Transaction
 suspend fun backupSnapshot(): BackupData = BackupData(backupTopics(), backupResources())

 @Transaction
 suspend fun restoreBackup(backup: BackupData): RestoreResult {
  val existingTopics = backupTopics().associateBy { it.copy(id = 0) }.toMutableMap()
  val existingResources = backupResources().map { it.copy(id = 0) }.toMutableSet()
  val ids = mutableMapOf<Long, Long>()
  var topicsAdded = 0
  var resourcesAdded = 0
  for (topic in backup.topics) {
   val key = topic.copy(id = 0)
   val existing = existingTopics[key]
   val id = if (existing != null) existing.id else {
    val created = insertTopic(key)
    existingTopics[key] = key.copy(id = created)
    topicsAdded++
    created
   }
   ids[topic.id] = id
  }
  for (resource in backup.resources) {
   val copy = resource.copy(id = 0, topicId = ids.getValue(resource.topicId))
   if (existingResources.add(copy)) {
    insertResource(copy)
    resourcesAdded++
   }
  }
  return RestoreResult(topicsAdded, resourcesAdded)
 }

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

 @Transaction
 suspend fun deleteTopicWithResources(topic: Topic) {
  deleteResourcesForTopic(topic.id)
  deleteTopic(topic)
 }
}
