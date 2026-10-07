package com.murcafes.notcatg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.murcafes.notcatg.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDatabase.get(this).noteDao()
        setContent { MaterialTheme { ThematicIndexApp(dao) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThematicIndexApp(dao: NoteDao) {
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var showTopicDialog by rememberSaveable { mutableStateOf(false) }
    var showResourceDialog by rememberSaveable { mutableStateOf(false) }
    var deleteTopic by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var showBackup by rememberSaveable { mutableStateOf(false) }
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
                    IconButton(onClick = { showBackup = true }) { Icon(Icons.Default.Settings, "Copias de seguridad") }
                },
                navigationIcon = {
                    if (selectedId != null) IconButton(onClick = { goBack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver a los temas")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (selectedId == null) showTopicDialog = true
                else if (selected != null) showResourceDialog = true
            }) { Icon(Icons.Default.Add, if (selectedId == null) "Nuevo tema" else "Añadir cita") }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize()) {
            if (selectedId == null) {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(),
                    label = { Text("Buscar temas") }, singleLine = true)
                Text("${topics.size} temas · Tu colección personal de citas y notas",
                    Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodySmall)
                if (filteredTopics.isEmpty()) Text(
                    if (query.isBlank()) "Crea tu primer tema con +." else "No hay temas que coincidan.")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(filteredTopics, key = { it.id }) { topic ->
                        Card(onClick = { selectedId = topic.id; query = "" },
                            modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp)) {
                                Text(topic.name, style = MaterialTheme.typography.titleMedium)
                                if (topic.description.isNotBlank()) Text(topic.description)
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
        label = { Text("Buscar cita, texto, autor o nota") }, singleLine = true)
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
        ConfirmDelete("Eliminar recurso", "¿Eliminar «${resource.reference}» de este tema?", saving,
            onDismiss = { deleting = null }, onConfirm = {
                onWrite({ deleting = null }, { dao.deleteResource(resource) })
            })
    }
}

@Composable
fun ResourceCard(resource: Resource, saving: Boolean, onFavorite: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by rememberSaveable(resource.id) { mutableStateOf(false) }
    Card(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(resource.reference, style = MaterialTheme.typography.titleMedium)
                    Text(resource.type, style = MaterialTheme.typography.labelMedium)
                    Text(if (expanded) "Ocultar contenido" else "Toca para ver el contenido",
                        style = MaterialTheme.typography.bodySmall)
                }
                IconButton(enabled = !saving, onClick = onFavorite) {
                    Icon(if (resource.favorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        if (resource.favorite) "Quitar de favoritos" else "Marcar como favorito")
                }
            }
            AnimatedVisibility(expanded) {
                Column {
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    if (resource.text.isNotBlank()) Text(resource.text)
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
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (original == null) "Añadir cita o recurso" else "Editar recurso") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tipo", style = MaterialTheme.typography.labelLarge)
                RESOURCE_TYPES.forEach { item ->
                    FilterChip(selected = type == item, enabled = !saving, onClick = { type = item }, label = { Text(item) })
                }
                OutlinedTextField(reference, { reference = it }, label = { Text("Referencia o título · ej. Mt 5,5") }, modifier = Modifier.fillMaxWidth(), enabled = !saving)
                if (type == "Biblia") {
                    NavarraBiblePanel(reference, enabled = !saving && body.isBlank()) { text, attribution ->
                        body = text
                        source = attribution
                    }
                    if (body.isNotBlank()) Text("Para consultar otra cita, vacía primero el texto. Se conserva el contenido actual.",
                        style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(body, { body = it }, label = { Text("Texto / descripción") }, modifier = Modifier.fillMaxWidth(), minLines = 3, enabled = !saving)
                OutlinedTextField(source, { source = it }, label = { Text("Fuente / autor / edición") }, modifier = Modifier.fillMaxWidth(), enabled = !saving)
                OutlinedTextField(note, { note = it }, label = { Text("Nota personal") }, modifier = Modifier.fillMaxWidth(), enabled = !saving)
            }
        },
        confirmButton = { Button(enabled = reference.isNotBlank() && !saving, onClick = {
            val base = original ?: Resource(topicId = 0, reference = "")
            onSave(base.copy(type = type, reference = reference.trim(), text = body.trim(), source = source.trim(),
                personalNote = note.trim(), updatedAt = System.currentTimeMillis()))
        }) { Text(if (saving) "Guardando…" else "Guardar") } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancelar") } })

}

@Composable
fun ConfirmDelete(title: String, message: String, saving: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text(title) }, text = { Text(message) },
        confirmButton = { Button(enabled = !saving, onClick = onConfirm) { Text("Eliminar") } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancelar") } })
}
