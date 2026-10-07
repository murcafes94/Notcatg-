package com.murcafes.notcatg.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor
import java.io.File
import java.util.zip.ZipFile

object NavarraEpub {
    const val MAX_EPUB_BYTES = 25 * 1024 * 1024
    private val marker = Regex("^tx-([^-]+)-(\\d+)-.*$")
    private val printedNumber = Regex("^\\(?([0-9]+[a-z]*)(?:[–-]([0-9]+))?")

    // Read only scripture blocks. Introductions, editorial notes, navigation and verse numbers are excluded.
    fun parseHtml(html: String): List<NavarraVerse> {
        val result = linkedMapOf<Triple<String, Int, String>, NavarraVerse>()
        var current: NavarraVerse? = null
        val text = StringBuilder()
        var skipDepth = 0
        fun flush() {
            val verse = current ?: return
            val body = text.toString().replace('\u00a0', ' ').replace(Regex("[ \\t]+"), " ")
                .replace(Regex(" *\\n *"), "\n").trim()
            if (body.isNotBlank()) {
                val key = Triple(verse.book, verse.chapter, verse.verse)
                val previous = result[key]
                result[key] = verse.copy(text = if (previous == null) body else previous.text + "\n" + body)
            }
            text.clear()
        }
        val document = Jsoup.parse(html)
        for (block in document.body().children()) {
            if (block.tagName() !in listOf("p", "h5")) continue
            NodeTraversor.traverse(object : NodeVisitor {
                override fun head(node: Node, depth: Int) {
                    if (node is Element) {
                        if (skipDepth > 0) { skipDepth++; return }
                        val match = marker.matchEntire(node.id())
                        if ((node.hasClass("ver") || node.hasClass("lnkver")) && match != null) {
                            flush()
                            val label = printedNumber.find(node.text().trim())
                                ?: throw IllegalArgumentException("Numeración bíblica incompatible")
                            current = NavarraVerse(match.groupValues[1], match.groupValues[2].toInt(), label.groupValues[1], "",
                                label.groupValues[1] + if (label.groupValues[2].isNotEmpty()) "–${label.groupValues[2]}" else "")
                            skipDepth = 1
                        } else if (node.hasClass("capital") || node.hasClass("tapcap")) {
                            skipDepth = 1
                        } else if (node.tagName() == "br" && current != null) text.append('\n')
                    } else if (node is TextNode && skipDepth == 0 && current != null) {
                        text.append(node.wholeText)
                    }
                }
                override fun tail(node: Node, depth: Int) {
                    if (node is Element && skipDepth > 0) skipDepth--
                }
            }, block)
            if (current != null) text.append('\n')
        }
        flush()
        // Some source passages print two verse numbers together. Both references resolve to that same passage.
        val verses = result.values.toMutableList()
        for (verse in result.values) {
            val range = Regex("^(\\d+)–(\\d+)$").matchEntire(verse.printedLabel) ?: continue
            val start = range.groupValues[1].toInt()
            val end = range.groupValues[2].toInt()
            require(end >= start && end - start < 20)
            for (number in start + 1..end) {
                if (Triple(verse.book, verse.chapter, number.toString()) !in result)
                    verses.add(verse.copy(verse = number.toString()))
            }
        }
        return verses
    }

    fun parse(file: File): List<NavarraVerse> {
        require(file.length() in 1..MAX_EPUB_BYTES.toLong()) { "El EPUB supera 25 MB." }
        val verses = mutableListOf<NavarraVerse>()
        ZipFile(file).use { zip ->
            val entries = zip.entries().asSequence().filter {
                Regex("Tx[0-9]+[a-z]?\\.html", RegexOption.IGNORE_CASE).matches(it.name.substringAfterLast('/'))
            }.sortedBy { it.name }.toList()
            require(entries.isNotEmpty() && entries.size <= 500) { "No es el formato de Navarra compatible." }
            var total = 0L
            for (entry in entries) {
                require(entry.size in 1..2_000_000)
                total += entry.size
                require(total <= 30_000_000)
                val html = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                verses += parseHtml(html)
            }
        }
        require(verses.size in 30000..50000 && verses.map { it.book }.toSet() == NAVARRA_BOOK_CODES) {
            "No se encontraron los 73 libros completos de Navarra."
        }
        require(verses.map { Triple(it.book, it.chapter, it.verse) }.toSet().size == verses.size) {
            "El EPUB contiene referencias duplicadas entre archivos."
        }
        return verses
    }
}
