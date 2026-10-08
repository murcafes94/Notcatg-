package com.murcafes.notcatg.data

import org.junit.Assert.*
import org.junit.Test

class ResourcePresentationTest {
    @Test fun usesAuthorThenReferenceThenTextForCardTitle() {
        val quote = Resource(topicId = 1, reference = "Libro, p. 12", text = "Contenido", source = "San Agustín\nEdición")
        assertEquals("San Agustín", quote.displayTitle())
        assertEquals("Libro, p. 12", quote.copy(source = "").displayTitle())
        assertEquals("Contenido", quote.copy(source = "", reference = "").displayTitle())
        assertEquals("Mi comentario", quote.copy(source = "", reference = "", text = "", personalNote = "Mi comentario").displayTitle())
    }
    @Test fun permitsReferenceFreeNotesButRejectsEmptyFields() {
        assertTrue(hasResourceContent("", "Pensamiento propio", ""))
        assertTrue(hasResourceContent("", "", "Comentario"))
        assertFalse(hasResourceContent(" ", "\n", "\t"))
    }
}
