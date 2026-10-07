package com.murcafes.notcatg.data

import java.text.Normalizer
import java.util.Locale

private fun key(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).replace(Regex("[\\s.]+"), "")

// Names shown in the provider's public book selector; aliases are Catholic abbreviations.
private val books = """
Génesis|Gn|Gen
Éxodo|Ex
Levítico|Lv|Lev
Números|Nm|Num
Deuteronomio|Dt|Deut
Josué|Jos
Jueces|Jue
Rut|Rt
Primer libro de Samuel|1 S|1 Sam
Segundo libro de Samuel|2 S|2 Sam
Primer libro de los Reyes|1 R|1 Re
Segundo libro de los Reyes|2 R|2 Re
Primer libro de las Crónicas|1 Cr|1 Cro
Segundo libro de las Crónicas|2 Cr|2 Cro
Esdras|Esd
Nehemías|Neh|Ne
Tobías|Tb|Tob
Judit|Jdt
Ester|Est
Primer libro de los Macabeos|1 M|1 Mac
Segundo libro de los Macabeos|2 M|2 Mac
Job|Jb
Salmos|Sal|Sl
Proverbios|Pr|Prov
Eclesiastés (Qohelet)|Qo|Ec|Eclesiastés
Cantar de los Cantares|Ct|Cant
Sabiduría|Sb|Sab
Eclesiástico (Sirácida)|Si|Eclo|Sir
Isaías|Is
Jeremías|Jr|Jer
Lamentaciones|Lm|Lam
Baruc|Ba|Bar
Ezequiel|Ez
Daniel|Dn|Dan
Oseas|Os
Joel|Jl
Amós|Am
Abdías|Abd
Jonás|Jon
Miqueas|Mi|Miq
Nahúm|Na|Nah
Habacuc|Ha|Hab
Sofonías|So|Sof
Ageo|Ag
Zacarías|Za|Zac
Malaquías|Ml|Mal
Evangelio según san Mateo|Mt|Mateo
Evangelio según san Marcos|Mc|Marcos
Evangelio según san Lucas|Lc|Lucas
Evangelio según san Juan|Jn|Juan
Hechos de los Apóstoles|Hch|Hechos
Romanos|Rm|Rom
Primera carta a los Corintios|1 Co|1 Cor
Segunda carta a los Corintios|2 Co|2 Cor
Gálatas|Ga|Gal
Efesios|Ef
Filipenses|Flp|Fil
Colosenses|Col
Primera carta a los Tesalonicenses|1 Ts|1 Tes
Segunda carta a los Tesalonicenses|2 Ts|2 Tes
Primera carta a Timoteo|1 Tm|1 Tim
Segunda carta a Timoteo|2 Tm|2 Tim
Tito|Tt|Tit
Filemón|Flm
Hebreos|Hb|Heb
Santiago|St|Stg|Sant
Primera carta de Pedro|1 P|1 Pe
Segunda carta de Pedro|2 P|2 Pe
Primera carta de Juan|1 Jn
Segunda carta de Juan|2 Jn
Tercera carta de Juan|3 Jn
Judas|Judas|Jud
Apocalipsis|Ap|Apoc
""".trimIndent().lines().flatMap { line ->
    val fields = line.split('|')
    fields.map { key(it) to fields.first() }
}.toMap()

data class BibleVerse(val label: String, val text: String)
data class BibleReference(val book: String, val chapter: Int, val first: Int, val last: Int, val query: String) {
    fun extract(verses: List<BibleVerse>): String? {
        val found = mutableMapOf<Int, String>()
        for (verse in verses) {
            val match = Regex("^(.+?)\\s+(\\d+),\\s*(\\d+)$").matchEntire(verse.label.trim()) ?: return null
            if (key(match.groupValues[1]) != key(book) || match.groupValues[2].toInt() != chapter) return null
            val number = match.groupValues[3].toInt()
            if (number !in first..last || verse.text.isBlank() || found.put(number, verse.text.trim()) != null) return null
        }
        if (found.keys != (first..last).toSet()) return null
        return (first..last).joinToString("\n") { number ->
            if (first == last) found.getValue(number) else "$number. ${found.getValue(number)}"
        }
    }
    companion object {
        fun parse(value: String): BibleReference? {
            val match = Regex("^(.+?)\\s+(\\d{1,3})\\s*[,：:]\\s*(\\d{1,3})(?:\\s*[-–]\\s*(\\d{1,3}))?$")
                .matchEntire(value.trim()) ?: return null
            val book = books[key(match.groupValues[1])] ?: return null
            val chapter = match.groupValues[2].toInt()
            val first = match.groupValues[3].toInt()
            val last = match.groupValues[4].toIntOrNull() ?: first
            if (chapter !in 1..150 || first < 1 || last < first || last - first >= 20) return null
            val query = "${match.groupValues[1]} $chapter,$first" + if (last != first) "-$last" else ""
            return BibleReference(book, chapter, first, last, query)
        }
    }
}
