package com.murcafes.notcatg.data

import java.text.Normalizer
import java.util.Locale

private fun key(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).replace(Regex("[\\s.]+"), "")

// Catholic book names and aliases. EPUB codes follow the Navarra verse markers.
private val bookRows = """
Génesis|Gn|Gen
Éxodo|Ex
Levítico|Lv|Lev
Números|Nm|Num
Deuteronomio|Dt|Deut
Josué|Jos
Jueces|Jue|Jc
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
Abdías|Abd|Ab
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
Judas|Judas|Jud|Jds
Apocalipsis|Ap|Apoc
""".trimIndent().lines().map { it.split('|') }
private val books = bookRows.flatMap { fields -> fields.map { key(it) to fields.first() } }.toMap()
private val codes = bookRows.associate { fields -> fields.first() to when (fields[1]) {
    "Jue" -> "Jc"
    "Abd" -> "Ab"
    "Judas" -> "Jds"
    "1 Cr" -> "1Cro"
    "2 Cr" -> "2Cro"
    "Neh" -> "Ne"
    else -> fields[1].replace(" ", "")
} }

internal val NAVARRA_BOOK_CODES = codes.values.toSet()

data class BibleReference(val book: String, val chapter: Int, val first: Int, val last: Int, val query: String, val suffix: String = "") {
    val epubBook: String get() = codes.getValue(book)
    companion object {
        fun parse(value: String): BibleReference? {
            val match = Regex("^(.+?)\\s+(\\d{1,3})\\s*[,：:]\\s*(\\d{1,3})([a-z]{0,2})(?:\\s*[-–]\\s*(\\d{1,3}))?$", RegexOption.IGNORE_CASE)
                .matchEntire(value.trim()) ?: return null
            val book = books[key(match.groupValues[1])] ?: return null
            val chapter = match.groupValues[2].toInt()
            val first = match.groupValues[3].toInt()
            val suffix = match.groupValues[4].lowercase(Locale.ROOT)
            val last = match.groupValues[5].toIntOrNull() ?: first
            if (chapter !in 1..150 || first < 1 || last < first || last - first >= 20 || (suffix.isNotEmpty() && last != first)) return null
            val query = "${match.groupValues[1]} $chapter,$first$suffix" + if (last != first) "-$last" else ""
            return BibleReference(book, chapter, first, last, query, suffix)
        }
    }
}
