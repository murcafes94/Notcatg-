package com.murcafes.notcatg

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.murcafes.notcatg.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun NavarraBiblePanel(reference: String, enabled: Boolean, onResult: (String, String) -> Unit) {
    val context = LocalContext.current
    val dao = remember { NavarraDatabase.get(context).verses() }
    val countFlow = remember(dao) { dao.count() }
    val count by countFlow.collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !working) {
            working = true
            message = "Preparando el índice de Navarra…"
            scope.launch {
                try {
                    val imported = withContext(Dispatchers.IO) {
                        val file = File.createTempFile("navarra-", ".epub", context.cacheDir)
                        try {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                file.outputStream().use { output ->
                                    val buffer = ByteArray(8192)
                                    var bytes = 0L
                                    while (true) {
                                        val size = input.read(buffer)
                                        if (size == -1) break
                                        bytes += size
                                        require(bytes <= NavarraEpub.MAX_EPUB_BYTES)
                                        output.write(buffer, 0, size)
                                    }
                                }
                            } ?: error("No se pudo abrir el EPUB")
                            val verses = NavarraEpub.parse(file)
                            dao.replaceBible(verses)
                            verses.size
                        } finally { file.delete() }
                    }
                    message = "Navarra preparada: $imported referencias disponibles sin internet."
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { message = "No se pudo importar. Selecciona el EPUB de Navarra compatible (hasta 25 MB). La biblioteca anterior se conserva." }
                finally { working = false }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (count == 0) "Importa una vez el EPUB de Navarra para buscar sin internet." else "Biblia de Navarra · $count referencias · Sin conexión",
            style = MaterialTheme.typography.bodySmall)
        OutlinedButton(enabled = enabled && !working, onClick = {
            importer.launch(arrayOf("application/epub+zip", "application/zip", "application/octet-stream"))
        }) { Text(if (count == 0) "Importar EPUB de Navarra" else "Volver a importar EPUB") }
        TextButton(enabled = enabled && !working && count > 0 && reference.isNotBlank(), onClick = {
            val request = BibleReference.parse(reference)
            if (request == null) {
                message = "Usa Mt 5,5, 1 Co 13,4-7 o Est 4,17a (hasta 20 versículos de un capítulo)."
            } else {
                working = true
                scope.launch {
                    try {
                        val rows = withContext(Dispatchers.IO) { dao.find(request.epubBook, request.chapter, request.navarraNumbers()) }
                        val text = request.navarraText(rows)
                        if (text == null) message = "No se encontró la cita completa. Revisa la referencia y la numeración de Navarra."
                        else {
                            onResult(text, "Sagrada Biblia · Universidad de Navarra · EUNSA\nEPUB local · ${request.query}")
                            message = "Cita recuperada. Pulsa Guardar para conservarla en este tema."
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { message = "No se pudo consultar el índice local. Intenta de nuevo." }
                    finally { working = false }
                }
            }
        }) { Text("Buscar cita en Navarra") }
        if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
    }
}
