package com.murcafes.notcatg

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun SettingsDialog(paletteId: String, mode: String, onPalette: (String) -> Unit, onMode: (String) -> Unit,
    onBackups: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(12.dp).navigationBarsPadding().widthIn(max = 600.dp).fillMaxWidth().fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Configuración", style = MaterialTheme.typography.headlineSmall)
                HorizontalDivider()
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Apariencia", style = MaterialTheme.typography.titleMedium)
                    listOf("system" to "Según el sistema", "light" to "Modo claro", "dark" to "Modo oscuro").forEach { (id, label) ->
                        // The whole row is a single accessible selection target.
                        OutlinedCard(onClick = { onMode(id) }, modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(if (mode == id) 2.dp else 1.dp,
                                if (mode == id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = mode == id, onClick = null)
                                Spacer(Modifier.width(12.dp))
                                Text(label)
                            }
                        }
                    }
                    Text("Paleta de colores", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium)
                    Text("El cambio se aplica al instante y queda guardado.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    THEME_PALETTES.forEach { palette ->
                        OutlinedCard(onClick = { onPalette(palette.id) }, modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(if (paletteId == palette.id) 2.dp else 1.dp,
                                if (paletteId == palette.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = paletteId == palette.id, onClick = null)
                                Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(palette.name, style = MaterialTheme.typography.titleSmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        palette.swatches.forEach { color -> Box(Modifier.size(22.dp).clip(CircleShape).background(color)) }
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                OutlinedButton(onClick = onBackups, modifier = Modifier.fillMaxWidth()) { Text("Copias de seguridad · Drive y archivos") }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Listo") }
            }
        }
    }
}
