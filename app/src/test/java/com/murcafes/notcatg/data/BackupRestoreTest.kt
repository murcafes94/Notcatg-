package com.murcafes.notcatg.data

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BackupRestoreTest {
    @Test fun restoreRemapsConflictingIdsAndDoesNotDuplicateASecondImport() = runBlocking {
        val dao = MemoryDao()
        dao.insertTopic(Topic(name = "Tema existente", createdAt = 1))
        dao.insertResource(Resource(topicId = 1, reference = "Nota existente", createdAt = 1, updatedAt = 1))
        val backup = BackupData(listOf(Topic(1, "Oración", createdAt = 2)),
            listOf(Resource(id = 1, topicId = 1, reference = "Cita", favorite = true, createdAt = 2, updatedAt = 2)))
        assertEquals(RestoreResult(1, 1), dao.restoreBackup(backup))
        assertEquals(RestoreResult(0, 0), dao.restoreBackup(backup))
        val snapshot = dao.backupSnapshot()
        assertEquals(2, snapshot.topics.size)
        assertEquals(2, snapshot.resources.size)
        val topic = snapshot.topics.single { it.name == "Oración" }
        val resource = snapshot.resources.single { it.reference == "Cita" }
        assertEquals(topic.id, resource.topicId)
        assertTrue(resource.favorite)
        assertTrue(snapshot.resources.any { it.reference == "Nota existente" })
    }
    @Test fun preservesAnEditedResourceAndImportsItsPreviousVersion() = runBlocking {
        val dao = MemoryDao()
        val topicId = dao.insertTopic(Topic(name = "Tema", createdAt = 1))
        val original = Resource(topicId = topicId, reference = "Cita", text = "Original", createdAt = 1, updatedAt = 1)
        val id = dao.insertResource(original)
        val backup = dao.backupSnapshot()
        dao.updateResource(original.copy(id = id, text = "Editado", updatedAt = 2))
        assertEquals(RestoreResult(0, 1), dao.restoreBackup(backup))
        assertEquals(setOf("Original", "Editado"), dao.backupResources().map { it.text }.toSet())
    }
}

private class MemoryDao : NoteDao {
    private val storedTopics = mutableListOf<Topic>()
    private val storedResources = mutableListOf<Resource>()
    override suspend fun backupTopics() = storedTopics.toList()
    override suspend fun backupResources() = storedResources.toList()
    override fun topics() = flowOf(storedTopics.toList())
    override fun searchTopics(q: String) = topics()
    override fun resources(topicId: Long) = flowOf(storedResources.filter { it.topicId == topicId })
    override fun searchResources(q: String) = flowOf(storedResources.toList())
    override suspend fun insertTopic(topic: Topic): Long {
        val id = (storedTopics.maxOfOrNull { it.id } ?: 0) + 1
        storedTopics.add(topic.copy(id = id)); return id
    }
    override suspend fun insertResource(resource: Resource): Long {
        val id = (storedResources.maxOfOrNull { it.id } ?: 0) + 1
        storedResources.add(resource.copy(id = id)); return id
    }
    override suspend fun updateTopic(topic: Topic) { storedTopics[storedTopics.indexOfFirst { it.id == topic.id }] = topic }
    override suspend fun updateResource(resource: Resource) { storedResources[storedResources.indexOfFirst { it.id == resource.id }] = resource }
    override suspend fun deleteTopic(topic: Topic) { storedTopics.removeAll { it.id == topic.id } }
    override suspend fun deleteResource(resource: Resource) { storedResources.removeAll { it.id == resource.id } }
    override suspend fun deleteResourcesForTopic(topicId: Long) { storedResources.removeAll { it.topicId == topicId } }
}
