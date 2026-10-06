package com.murcafes.notcatg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.murcafes.notcatg.data.*
import kotlinx.coroutines.launch

class MainActivity: ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val dao = AppDatabase.get(this).noteDao()
  setContent { MaterialTheme { NotesApp(dao) } }
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun NotesApp(dao: NoteDao) {
 var query by remember { mutableStateOf("") }
 val notes by dao.search(query).collectAsState(initial=emptyList())
 var editing by remember { mutableStateOf<Note?>(null) }
 var creating by remember { mutableStateOf(false) }
 val scope = rememberCoroutineScope()

 Scaffold(
  topBar={ TopAppBar(title={Text("Mis apuntes")}) },
  floatingActionButton={ FloatingActionButton(onClick={creating=true}) { Icon(Icons.Default.Add,"Nuevo apunte") } }
 ) { pad ->
  Column(Modifier.padding(pad).padding(16.dp)) {
   OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),label={Text("Buscar en todos los apuntes")},singleLine=true)
   Spacer(Modifier.height(12.dp))
   if(notes.isEmpty()) Box(Modifier.fillMaxSize()){ Text(if(query.isBlank()) "Todavía no hay apuntes. Crea el primero con +." else "No encontramos coincidencias.") }
   else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
    items(notes,key={it.id}) { n ->
     Card(onClick={editing=n},modifier=Modifier.fillMaxWidth()) {
      Row(Modifier.padding(16.dp).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
       Column(Modifier.weight(1f)) {
        Text(n.title,style=MaterialTheme.typography.titleMedium)
        if(n.category.isNotBlank()) Text(n.category,style=MaterialTheme.typography.labelMedium)
        if(n.content.isNotBlank()) Text(n.content.take(120),maxLines=2)
        if(n.tags.isNotBlank()) Text(n.tags,style=MaterialTheme.typography.labelSmall)
       }
       IconButton(onClick={scope.launch { dao.update(n.copy(favorite=!n.favorite,updatedAt=System.currentTimeMillis())) }}) {
        Icon(if(n.favorite) Icons.Default.Star else Icons.Outlined.StarBorder,"Favorito")
       }
      }
     }
    }
   }
  }
 }
 if(creating) NoteEditor(null,onDismiss={creating=false},onSave={scope.launch{dao.insert(it)};creating=false})
 editing?.let { n -> NoteEditor(n,onDismiss={editing=null},onSave={scope.launch{dao.update(it)};editing=null},onDelete={scope.launch{dao.delete(n)};editing=null}) }
}

@Composable fun NoteEditor(note: Note?, onDismiss:()->Unit, onSave:(Note)->Unit, onDelete:(()->Unit)?=null) {
 var title by remember(note){mutableStateOf(note?.title?:"")}
 var content by remember(note){mutableStateOf(note?.content?:"")}
 var category by remember(note){mutableStateOf(note?.category?:"")}
 var tags by remember(note){mutableStateOf(note?.tags?:"")}
 AlertDialog(
  onDismissRequest=onDismiss,
  title={Text(if(note==null)"Nuevo apunte" else "Editar apunte")},
  text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
   OutlinedTextField(title,{title=it},label={Text("Título")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(content,{content=it},label={Text("Contenido")},modifier=Modifier.fillMaxWidth(),minLines=5)
   OutlinedTextField(category,{category=it},label={Text("Categoría (opcional)")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(tags,{tags=it},label={Text("Etiquetas, separadas por coma")},modifier=Modifier.fillMaxWidth())
   if(onDelete!=null) TextButton(onClick=onDelete){Text("Eliminar")}
  }},
  confirmButton={Button(enabled=title.isNotBlank()||content.isNotBlank(),onClick={
   val now=System.currentTimeMillis()
   onSave(note?.copy(title=title,content=content,category=category,tags=tags,updatedAt=now) ?: Note(title=title,content=content,category=category,tags=tags,createdAt=now,updatedAt=now))
  }){Text("Guardar")}},
  dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}}
 )
}
