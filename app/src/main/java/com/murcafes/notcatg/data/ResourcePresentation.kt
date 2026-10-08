package com.murcafes.notcatg.data

/** A source by itself does not create an empty card. Reference is optional. */
fun hasResourceContent(reference: String, text: String, note: String): Boolean =
    reference.isNotBlank() || text.isNotBlank() || note.isNotBlank()

fun Resource.displayTitle(): String = source.trim().lineSequence().firstOrNull { it.isNotBlank() }?.trim()
    ?: reference.trim().ifBlank {
    val preview = text.ifBlank { personalNote }.trim().replace(Regex("\\s+"), " ")
    if (preview.isBlank()) type else if (preview.length <= 80) preview else preview.take(80).trimEnd() + "…"
}
