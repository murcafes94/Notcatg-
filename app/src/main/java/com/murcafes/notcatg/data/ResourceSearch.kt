package com.murcafes.notcatg.data

import java.text.Normalizer
import java.util.Locale

val RESOURCE_TYPES = listOf("Biblia", "Santo / Magisterio", "Libro", "Pensamiento", "Nota propia")

private fun String.searchKey(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .lowercase(Locale.ROOT)

fun matchesSearch(query: String, vararg fields: String): Boolean {
    val words = query.searchKey().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val text = fields.joinToString(" ").searchKey()
    return words.all { text.contains(it) }
}

fun Resource.matches(query: String, type: String = "", favoritesOnly: Boolean = false): Boolean =
    (type.isEmpty() || this.type == type) && (!favoritesOnly || favorite) &&
        matchesSearch(query, reference, text, source, personalNote, this.type)
