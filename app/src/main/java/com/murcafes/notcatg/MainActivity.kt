package com.murcafes.notcatg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.murcafes.notcatg.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDatabase.get(this).noteDao()
        val preferences = getSharedPreferences("appearance", MODE_PRIVATE)
        setContent {
            var palette by rememberSaveable { mutableStateOf(preferences.getString("palette", "parchment") ?: "parchment") }
            var mode by rememberSaveable { mutableStateOf(preferences.getString("mode", "system") ?: "system") }
            NotcatgTheme(palette, mode) {
                ThematicIndexApp(dao, palette, mode,
                    onPalette = { palette = it; preferences.edit().putString("palette", it).apply() },
                    onMode = { mode = it; preferences.edit().putString("mode", it).apply() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThematicIndexApp(dao: NoteDao, paletteId: String, mode: String, onPalette: (String) -> Unit, onMode: (String) -> Unit) {
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var showTopicDialog by rememberSaveable { mutableStateOf(false) }
    var showResourceDialog by rememberSaveable { mutableStateOf(false) }
    var deleteTopic by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var showBackup by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val topicFlow = remember(dao) { dao.topics() }
    val topics by topicFlow.collectAsState(initial = emptyList())
    val selected = topics.firstOrNull { it.id == selectedId }
    val filteredTopics = remember(topics, query) {
        topics.filter { matchesSearch(query, it.name, it.description) }
    }
    fun write(onSuccess: () -> Unit = {}, operation: suspend () -> Unit) {
        if (saving) return
        saving = true
        scope.launch {
            try {
                operation()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbar.showSnackbar("No se pudo guardar el cambio. Intenta de nuevo.")
            } finally {
                saving = false
            }
        }
    }
    fun goBack() { selectedId = null; query = "" }
    BackHandler(enabled = selectedId != null) { goBack() }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(selected?.name ?: "Índice temático") },
                actions = {
                    IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "Configuración") }
                },
                navigationIcon = {
                    if (selectedId != null) IconButton(onClick = { goBack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver a los temas")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = {
                if (selectedId == null) showTopicDialog = true
                else if (selected != null) showResourceDialog = true
            }, icon = { Icon(Icons.Default.Add, null) },
                text = { Text(if (selectedId == null) "Nuevo tema" else "Añadir cita") })
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize()) {
            if (selectedId == null) {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(),
                    label = { Text("Buscar temas") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
                Text("${topics.size} temas · Citas, lecturas y reflexiones",
                    Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodySmall)
                if (filteredTopics.isEmpty()) Text(
                    if (query.isBlank()) "Crea tu primer tema con +." else "No hay temas que coincidan.")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(filteredTopics, key = { it.id }) { topic ->
                        Card(onClick = { selectedId = topic.id; query = "" },
                            modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(topic.name, style = MaterialTheme.typography.titleLarge)
                                    if (topic.description.isNotBlank()) Text(topic.description, maxLines = 2,
                                        overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.ChevronRight, "Abrir tema", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            } else if (selected != null) {
                val topic = selected
                if (topic.description.isNotBlank()) Text(topic.description, Modifier.padding(top = 8.dp))
                Row {
                    TextButton(enabled = !saving, onClick = { showTopicDialog = true }) { Text("Editar tema") }
                    TextButton(enabled = !saving, onClick = { deleteTopic = true }) { Text("Eliminar tema") }
                }
                TopicScreen(topic, dao, saving, onWrite = { done, operation -> write(done, operation) })
            } else Text("Cargando tema…")
        }
    }
    if (showTopicDialog) TopicDialog(selected, saving, onDismiss = { showTopicDialog = false }) { name, description ->
        val topic = selected
        write(onSuccess = { showTopicDialog = false }) {
            if (topic == null) dao.insertTopic(Topic(name = name, description = description))
            else dao.updateTopic(topic.copy(name = name, description = description))
        }
    }
    if (showSettings) SettingsDialog(paletteId, mode, onPalette, onMode,
        onBackups = { showSettings = false; showBackup = true }, onDismiss = { showSettings = false })
    if (showBackup) BackupDialog(dao, onDismiss = { showBackup = false })
    if (showResourceDialog && selected != null) {
        val topicId = selected.id
        ResourceDialog(null, saving, onDismiss = { showResourceDialog = false }) { resource ->
            write(onSuccess = { showResourceDialog = false }) { dao.insertResource(resource.copy(topicId = topicId)) }
        }
    }
    if (deleteTopic && selected != null) {
        val topic = selected
        ConfirmDelete("Eliminar tema", "Se eliminará «${topic.name}» junto con todas sus citas y notas.", saving,
            onDismiss = { deleteTopic = false }, onConfirm = {
                write(onSuccess = { deleteTopic = false; goBack() }) { dao.deleteTopicWithResources(topic) }
            })
    }
}

@Composable
fun TopicScreen(topic: Topic, dao: NoteDao, saving: Boolean,
    onWrite: (() -> Unit, suspend () -> Unit) -> Unit) {
    val resourceFlow = remember(dao, topic.id) { dao.resources(topic.id) }
    val resources by resourceFlow.collectAsState(initial = emptyList())
    var query by rememberSaveable(topic.id) { mutableStateOf("") }
    var type by rememberSaveable(topic.id) { mutableStateOf("") }
    var favoritesOnly by rememberSaveable(topic.id) { mutableStateOf(false) }
    var editing by remember(topic.id) { mutableStateOf<Resource?>(null) }
    var deleting by remember(topic.id) { mutableStateOf<Resource?>(null) }
    val filtered = remember(resources, query, type, favoritesOnly) {
        resources.filter { it.matches(query, type, favoritesOnly) }
    }
    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(),
        label = { Text("Buscar en este tema") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = type.isEmpty(), onClick = { type = "" }, label = { Text("Todos") })
        RESOURCE_TYPES.forEach { item ->
            FilterChip(selected = type == item, onClick = { type = item }, label = { Text(item) })
        }
        FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly }, label = { Text("Favoritos") })
    }
    Text("${filtered.size} de ${resources.size} recursos", Modifier.padding(vertical = 8.dp),
        style = MaterialTheme.typography.bodySmall)
    if (filtered.isEmpty()) Text(if (resources.isEmpty()) "Añade citas y notas con +." else "No hay recursos con estos filtros.")
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
        items(filtered, key = { it.id }) { resource ->
            ResourceCard(resource, saving,
                onFavorite = { onWrite({}, { dao.updateResource(resource.copy(favorite = !resource.favorite, updatedAt = System.currentTimeMillis())) }) },
                onEdit = { editing = resource }, onDelete = { deleting = resource })
        }
    }
    editing?.let { original ->
        ResourceDialog(original, saving, onDismiss = { editing = null }) { resource ->
            onWrite({ editing = null }, { dao.updateResource(resource) })
        }
    }
    deleting?.let { resource ->
        ConfirmDelete("Eliminar recurso", "¿Eliminar «${resource.displayTitle()}» de este tema?", saving,
            onDismiss = { deleting = null }, onConfirm = {
                onWrite({ deleting = null }, { dao.deleteResource(resource) })
            })
    }
}

@Composable
fun ResourceCard(resource: Resource, saving: Boolean, onFavorite: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by rememberSaveable(resource.id) { mutableStateOf(false) }
    val importanceColor = importanceColor(resource.importance)
    Card(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth(),
        border = importanceColor?.let { BorderStroke(2.dp, it) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    if (importanceColor != null) {
                        Surface(color = importanceColor.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
                            Text("Importancia ${importanceLabel(resource.importance).lowercase()}", Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall, color = importanceColor)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(resource.displayTitle(), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(listOf(resource.type, resource.reference.takeIf { it.isNotBlank() && resource.source.isNotBlank() }).filterNotNull().joinToString(" · "), Modifier.padding(vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(if (expanded) "Ocultar contenido" else "Ver contenido",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(enabled = !saving, onClick = onFavorite) {
                    Icon(if (resource.favorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        if (resource.favorite) "Quitar de favoritos" else "Marcar como favorito")
                }
            }
            AnimatedVisibility(expanded) {
                Column {
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    if (resource.text.isNotBlank()) Text(resource.text, style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Serif)
                    if (resource.source.isNotBlank()) Text("Fuente: ${resource.source}", Modifier.padding(top = 6.dp))
                    if (resource.personalNote.isNotBlank()) Text("Nota personal: ${resource.personalNote}", Modifier.padding(top = 6.dp))
                    Row {
                        TextButton(enabled = !saving, onClick = onEdit) { Text("Editar") }
                        TextButton(enabled = !saving, onClick = onDelete) { Text("Eliminar") }
                    }
                }
            }
        }
    }
}

@Composable
fun TopicDialog(original: Topic?, saving: Boolean, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by rememberSaveable(original?.id) { mutableStateOf(original?.name ?: "") }
    var description by rememberSaveable(original?.id) { mutableStateOf(original?.description ?: "") }
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (original == null) "Nuevo tema" else "Editar tema") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Tema") }, modifier = Modifier.fillMaxWidth(), enabled = !saving)
                OutlinedTextField(description, { description = it }, label = { Text("Descripción opcional") }, modifier = Modifier.fillMaxWidth(), enabled = !saving)
            }
        },
        confirmButton = { Button(enabled = name.isNotBlank() && !saving,
            onClick = { onSave(name.trim(), description.trim()) }) { Text(if (saving) "Guardando…" else "Guardar") } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
fun ResourceDialog(original: Resource?, saving: Boolean, onDismiss: () -> Unit, onSave: (Resource) -> Unit) {
    var type by rememberSaveable(original?.id) { mutableStateOf(original?.type ?: "Biblia") }
    var reference by rememberSaveable(original?.id) { mutableStateOf(original?.reference ?: "") }
    var body by rememberSaveable(original?.id) { mutableStateOf(original?.text ?: "") }
    var source by rememberSaveable(original?.id) { mutableStateOf(original?.source ?: "") }
    var note by rememberSaveable(original?.id) { mutableStateOf(original?.personalNote ?: "") }
    var importance by rememberSaveable(original?.id) { mutableStateOf(original?.importance ?: "") }
    var showNote by rememberSaveable(original?.id) { mutableStateOf(note.isNotBlank()) }
    Dialog(onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.padding(horizontal = 12.dp).imePadding().navigationBarsPadding()
            .widthIn(max = 680.dp).fillMaxWidth().fillMaxHeight(0.92f),
            shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (original == null) "Nuevo recurso" else "Editar recurso", style = MaterialTheme.typography.headlineSmall)
                        Text("Guarda lo que te ayude a pensar y compartir", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(enabled = !saving, onClick = onDismiss) { Icon(Icons.Default.Close, "Cerrar") }
                }
                HorizontalDivider()
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("¿Qué quieres guardar?", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RESOURCE_TYPES.forEach { item ->
                            FilterChip(selected = type == item, enabled = !saving, onClick = { type = item }, label = { Text(item) })
                        }
                    }
                    OutlinedTextField(body, { body = it }, label = { Text("Texto de la cita o pensamiento") },
                        modifier = Modifier.fillMaxWidth(), minLines = 4, enabled = !saving)
                    OutlinedTextField(source, { source = it }, label = { Text("Fuente o autor (opcional)") },
                        modifier = Modifier.fillMaxWidth(), enabled = !saving)
                    OutlinedTextField(reference, { reference = it },
                        label = { Text("Referencia o título (opcional)") },
                        supportingText = { Text(if (type == "Biblia") "Ej. Mt 5,5 · Se usa para buscar en Navarra" else "Ej. título del libro, página o nombre de la cita") },
                        modifier = Modifier.fillMaxWidth(), enabled = !saving)
                    if (type == "Biblia") {
                        NavarraBiblePanel(reference, enabled = !saving && body.isBlank()) { text, attribution ->
                            body = text
                            source = attribution
                        }
                        if (body.isNotBlank()) Text("Para buscar otra cita, vacía primero el texto.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Importancia (opcional)", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("", "low", "medium", "high").forEach { level ->
                            val color = importanceColor(level)
                            FilterChip(selected = importance == level, enabled = !saving, onClick = { importance = level },
                                label = { Text(importanceLabel(level)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color?.copy(alpha = 0.18f) ?: MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = color ?: MaterialTheme.colorScheme.onSecondaryContainer))
                        }
                    }
                    HorizontalDivider()
                    TextButton(enabled = !saving, onClick = { showNote = !showNote }, modifier = Modifier.fillMaxWidth()) {
                        Icon(if (showNote) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (showNote) "Ocultar comentario personal" else if (note.isBlank()) "Añadir comentario personal" else "Ver comentario personal")
                    }
                    AnimatedVisibility(showNote) {
                        OutlinedTextField(note, { note = it }, label = { Text("Tu comentario (opcional)") },
                            modifier = Modifier.fillMaxWidth(), minLines = 3, enabled = !saving)
                    }
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancelar") }
                    Button(modifier = Modifier.weight(1f), enabled = hasResourceContent(reference, body, note) && !saving, onClick = {
                        val base = original ?: Resource(topicId = 0, reference = "")
                        onSave(base.copy(type = type, reference = reference.trim(), text = body.trim(), source = source.trim(),
                            personalNote = note.trim(), importance = importance, updatedAt = System.currentTimeMillis()))
                    }) { Text(if (saving) "Guardando…" else "Guardar recurso") }
                }
            }
        }
    }
}

@Composable
fun ConfirmDelete(title: String, message: String, saving: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text(title) }, text = { Text(message) },
        confirmButton = { Button(enabled = !saving, onClick = onConfirm) { Text("Eliminar") } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancelar") } })
}
