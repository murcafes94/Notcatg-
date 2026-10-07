package com.murcafes.notcatg

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

@Composable
fun NavarraBiblePanel(reference: String, enabled: Boolean, onResult: (String, String) -> Unit) {
    val context = LocalContext.current
    val dao = remember { NavarraDatabase.get(context).verses() }
    val countFlow = remember(dao) { dao.count() }
    val count by countFlow.collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    LaunchedEffect(dao) {
        working = true
        message = "Preparando la Biblia de Navarra incluida…"
        try {
            BundledNavarra.prepare(context)
            message = "Biblia de Navarra lista para buscar sin internet."
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { message = "No se pudo preparar la Biblia incluida. Cierra y vuelve a abrir esta pantalla para reintentar." }
        finally { working = false }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (count == 0) "Biblia de Navarra incluida · Preparación automática" else "Biblia de Navarra · $count referencias · Sin conexión",
            style = MaterialTheme.typography.bodySmall)
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
                            onResult(text, "Sagrada Biblia · Universidad de Navarra · EUNSA\nBiblia integrada · ${request.query}")
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
