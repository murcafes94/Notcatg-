package com.murcafes.notcatg.data

import org.junit.Assert.*
import org.junit.Test

class NavarraEpubTest {
    @Test fun excludesHeadingsNumbersAndFootnotesAndKeepsContinuationLines() {
        val html = """<html><body><h1>Título</h1><h3><a id="tx-Mt-5-1">Título editorial</a></h3>
            <p class="tx1"><b class="capital"><i>5</i><i>Mt</i></b><a class="lnkver" id="tx-Mt-5-1m">1</a> Texto <i>primero</i>.</p>
            <p class="tx">Continuación. <b class="ver" id="tx-Mt-5-2">2</b> Segundo texto.</p>
            <h3>Encabezado que no debe copiarse</h3><h5 class="izqsal"><i><b class="ver" id="tx-Mt-5-3">3</b></i>Poema</h5>
            <h5 class="s">segunda línea</h5><h6>navegación</h6></body></html>"""
        val verses = NavarraEpub.parseHtml(html)
        assertEquals(3, verses.size)
        assertEquals("Texto primero.\nContinuación.", verses[0].text)
        assertEquals("Segundo texto.", verses[1].text)
        assertEquals("Poema\nsegunda línea", verses[2].text)
    }
    @Test fun handlesLetteredVersesAlternativeNumbersAndGroupedVerses() {
        val verses = NavarraEpub.parseHtml("""<body>
            <p><b class="ver" id="tx-Est-4-17am">17a</b>Adición A.</p>
            <p><b class="ver" id="tx-Tb-6-_2">2 (1)</b>Otra numeración.</p>
            <p><b class="ver" id="tx-Si-9-10">10–11</b>Pasaje conjunto.</p>
            </body>""")
        assertEquals("17a", verses[0].verse)
        assertEquals("2", verses[1].verse)
        assertEquals(setOf("10", "11"), verses.filter { it.book == "Si" }.map { it.verse }.toSet())
    }
    @Test fun joinsFragmentsWithTheSamePrintedReference() {
        val verses = NavarraEpub.parseHtml("""<body><p><b class="ver" id="tx-Tb-4-7">7</b>Parte A.</p>
            <p><b class="ver" id="tx-Tb-4-7p">(7)</b>Parte B.</p></body>""")
        assertEquals(1, verses.size)
        assertEquals("Parte A.\nParte B.", verses.single().text)
    }
}
