package com.murcafes.notcatg.data

import org.junit.Assert.*
import org.junit.Test

class BibleReferenceTest {
    @Test fun parsesCatholicAliasesIntoNavarraCodes() {
        assertEquals("Mt", BibleReference.parse("Mt 5,5")!!.epubBook)
        assertEquals("1Co", BibleReference.parse("1 Co 13:4–7")!!.epubBook)
        assertEquals("Sb", BibleReference.parse("Sab 1,1")!!.epubBook)
        assertEquals("1Jn", BibleReference.parse("1Jn 1,1")!!.epubBook)
        assertEquals("Jc", BibleReference.parse("Jue 1,1")!!.epubBook)
        assertEquals("1Cro", BibleReference.parse("1 Cr 1,1")!!.epubBook)
        assertEquals(listOf("17a"), BibleReference.parse("Est 4,17a")!!.navarraNumbers())
    }
    @Test fun rejectsInvalidOrUnsupportedReferences() {
        listOf("Mt 0,1", "Mt 5,0", "Mt 5,7-4", "Mt 5,1-30", "Mt 5", "Mt 5,4;6", "Otro 5,4", "Est 4,17a-18").forEach {
            assertNull(it, BibleReference.parse(it))
        }
    }
    @Test fun requiresAllRequestedVersesAndKeepsTheirOrder() {
        val reference = BibleReference.parse("1 Co 13,4-5")!!
        val rows = listOf(NavarraVerse("1Co", 13, "5", "Texto B"), NavarraVerse("1Co", 13, "4", "Texto A"))
        assertEquals("4. Texto A\n5. Texto B", reference.navarraText(rows))
        assertNull(reference.navarraText(rows.take(1)))
        assertNull(reference.navarraText(rows + rows.first()))
        assertNull(reference.navarraText(rows.map { it.copy(chapter = 12) }))
        assertNull(reference.navarraText(rows.map { it.copy(book = "2Co") }))
    }
    @Test fun keepsCombinedVerseNumberingWithoutDuplicatingText() {
        val rows = listOf(NavarraVerse("Si", 9, "10", "Pasaje conjunto", "10–11"), NavarraVerse("Si", 9, "11", "Pasaje conjunto", "10–11"))
        assertEquals("10–11. Pasaje conjunto", BibleReference.parse("Si 9,10-11")!!.navarraText(rows))
    }
}
