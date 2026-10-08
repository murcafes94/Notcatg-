package com.murcafes.notcatg.data

import org.junit.Assert.*
import org.junit.Test

class BackupCodecTest {
    private val sample = BackupData(
        listOf(Topic(id = 4, name = "Oración", description = "Jóvenes", createdAt = 100)),
        listOf(Resource(id = 8, topicId = 4, type = "Biblia", reference = "Mt 5,4", text = "Texto de prueba\nSegunda línea",
            source = "Fuente de prueba", personalNote = "Mi nota", favorite = true, createdAt = 100, updatedAt = 200)))

    @Test fun roundTripPreservesAllFieldsAndUnicode() {
        assertEquals(sample, BackupCodec.decode(BackupCodec.encode(sample)))
        assertEquals(BackupData(emptyList(), emptyList()), BackupCodec.decode(BackupCodec.encode(BackupData(emptyList(), emptyList()))))
    }
    @Test fun restoresTextAndCommentWithoutReference() {
        listOf(
            sample.resources.single().copy(reference = "", personalNote = ""),
            sample.resources.single().copy(reference = "", text = "", personalNote = "Mi reflexión")
        ).forEach { resource ->
            val data = sample.copy(resources = listOf(resource))
            assertEquals(data, BackupCodec.decode(BackupCodec.encode(data)))
        }
        val empty = sample.copy(resources = listOf(sample.resources.single().copy(reference = " ", text = "", personalNote = "")))
        try { BackupCodec.decode(BackupCodec.encode(empty)); fail("Aceptó un recurso sin contenido") }
        catch (expected: IllegalArgumentException) { }
    }
    @Test fun rejectsOrphansDuplicateIdsAndUnsupportedVersions() {
        val invalid = listOf(
            BackupCodec.encode(sample.copy(topics = emptyList())),
            BackupCodec.encode(sample.copy(topics = sample.topics + sample.topics)),
            BackupCodec.encode(sample.copy(resources = sample.resources + sample.resources)),
            BackupCodec.encode(sample).replace("\"version\": 1", "\"version\": 2"),
            "{}", "Esto no es JSON")
        invalid.forEach { text ->
            try { BackupCodec.decode(text); fail("Aceptó un respaldo inválido") }
            catch (expected: Exception) { /* The caller reports an invalid backup without touching the database. */ }
        }
    }
    @Test fun rejectsOversizedFilesBeforeParsing() {
        try { BackupCodec.decode("x".repeat(BackupCodec.MAX_BYTES + 1)); fail("Aceptó un archivo demasiado grande") }
        catch (expected: IllegalArgumentException) { }
    }
}
