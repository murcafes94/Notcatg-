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
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupDialog(dao: NoteDao, onDismiss: () -> Unit) {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<BackupData?>(null) }
    fun work(operation: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try { operation() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { message = "No se pudo completar la operación. Revisa el archivo, el espacio disponible o la conexión de Drive." }
            finally { busy = false }
        }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) work {
            withContext(Dispatchers.IO) {
                val bytes = BackupCodec.encode(dao.backupSnapshot()).toByteArray(Charsets.UTF_8)
                require(bytes.size <= BackupCodec.MAX_BYTES)
                val output = resolver.openOutputStream(uri, "wt") ?: error("No se pudo abrir el destino")
                output.use { it.write(bytes) }
            }
            message = "Respaldo guardado en el destino seleccionado. Si elegiste Drive, su aplicación completará la subida."
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) work {
            pending = null
            pending = withContext(Dispatchers.IO) {
                val input = resolver.openInputStream(uri) ?: error("No se pudo abrir el respaldo")
                val bytes = input.use {
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = it.read(buffer)
                        if (count == -1) break
                        require(output.size() + count <= BackupCodec.MAX_BYTES)
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                BackupCodec.decode(bytes.toString(Charsets.UTF_8))
            }
            message = "Respaldo verificado. Revisa su contenido y pulsa Restaurar."
        }
    }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Copias de seguridad") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Guarda temas, citas, fuentes, notas y favoritos en un archivo. En el selector de Android, elige Google Drive o una carpeta del dispositivo.")
                Text("Para usar Drive, su aplicación debe estar instalada y configurada. Esta copia es manual; no se crea automáticamente.", style = MaterialTheme.typography.bodySmall)
                Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                    pending = null
                    val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.ROOT).format(Date())
                    export.launch("Notcatg_respaldo_$date.json")
                }) { Text("Guardar respaldo") }
                OutlinedButton(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                    pending = null
                    import.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
                }) { Text("Abrir respaldo") }
                pending?.let { backup ->
                    Text("${backup.topics.size} temas y ${backup.resources.size} recursos. Se añadirán los que falten, sin borrar la colección actual.")
                    Button(enabled = !busy, onClick = {
                        work {
                            val result = withContext(Dispatchers.IO) { dao.restoreBackup(backup) }
                            pending = null
                            message = "Restauración completa: ${result.topicsAdded} temas y ${result.resourcesAdded} recursos añadidos."
                        }
                    }) { Text("Restaurar") }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (message.isNotBlank()) Text(message)
            }
        }, confirmButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cerrar") } })
}
