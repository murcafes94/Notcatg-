package com.murcafes.notcatg.data

import org.junit.Assert.*
import org.junit.Test

class BibleReferenceTest {
    @Test fun parsesCatholicAbbreviationsAndRanges() {
        assertEquals("Evangelio según san Mateo", BibleReference.parse("Mt 5,4")!!.book)
        assertEquals(7, BibleReference.parse("1 Co 13:4–7")!!.last)
        assertEquals("Sabiduría", BibleReference.parse("Sab 1,1")!!.book)
        assertEquals("Primera carta de Juan", BibleReference.parse("1Jn 1,1")!!.book)
    }
    @Test fun rejectsInvalidOrUnsupportedReferences() {
        listOf("Mt 0,1", "Mt 5,0", "Mt 5,7-4", "Mt 5,1-30", "Mt 5", "Mt 5,4;6", "Otro 5,4").forEach {
            assertNull(it, BibleReference.parse(it))
        }
    }
    @Test fun requiresExactBookChapterAndCompleteRange() {
        val reference = BibleReference.parse("1 Co 13,4-5")!!
        val verses = listOf(BibleVerse("Primera carta a los Corintios 13, 4", "Texto A"), BibleVerse("Primera carta a los Corintios 13, 5", "Texto B"))
        assertEquals("4. Texto A\n5. Texto B", reference.extract(verses))
        assertNull(reference.extract(verses.take(1)))
        assertNull(reference.extract(verses + verses.first()))
        assertNull(reference.extract(listOf(BibleVerse("Primera carta a los Corintios 12, 4", "Texto A"))))
        assertNull(reference.extract(listOf(BibleVerse("Segunda carta a los Corintios 13, 4", "Texto A"))))
        assertNull(reference.extract(verses.map { it.copy(text = "") }))
    }
}
