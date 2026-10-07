package com.murcafes.notcatg.data

import org.junit.Assert.*
import org.junit.Test

class ResourceSearchTest {
    private val resource = Resource(id = 7, topicId = 3, type = "Libro", reference = "La oración",
        text = "Una reflexión sobre la fe", source = "José García", personalNote = "Para jóvenes", favorite = true)

    @Test fun findsReferenceTextAuthorAndPersonalNoteWithoutAccents() {
        listOf("ORACION", "reflexion", "jose garcia", "jovenes", "  fe   jose ").forEach {
            assertTrue("No encontró: $it", resource.matches(it))
        }
        assertFalse(resource.matches("esperanza"))
        assertFalse(resource.matches("fe desconocido"))
    }

    @Test fun combinesTypeFavoritesAndTextFilters() {
        assertTrue(resource.matches("", "Libro", true))
        assertFalse(resource.matches("", "Biblia", true))
        assertFalse(resource.copy(favorite = false).matches("", "Libro", true))
        assertTrue(resource.copy(favorite = false).matches("   "))
    }

    @Test fun searchesTopicNamesAndDescriptions() {
        assertTrue(matchesSearch("jovenes fe", "Jóvenes", "Problemas con la fe"))
        assertFalse(matchesSearch("vocacion", "Jóvenes", "Problemas con la fe"))
    }
}
