package com.murcafes.notcatg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.murcafes.notcatg.data.*
import kotlinx.coroutines.launch

class MainActivity: ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val dao=AppDatabase.get(this).noteDao()
  setContent { MaterialTheme { ThematicIndexApp(dao) } }
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ThematicIndexApp(dao: NoteDao) {
 var selected by remember { mutableStateOf<Topic?>(null) }
 var query by remember { mutableStateOf("") }
 var newTopic by remember { mutableStateOf(false) }
 var newResource by remember { mutableStateOf(false) }
 val scope=rememberCoroutineScope()
 val topics by dao.searchTopics(query).collectAsState(initial=emptyList())

 Scaffold(
  topBar={TopAppBar(
   navigationIcon={
    if(selected!=null) {
     IconButton(onClick={selected=null;query=""}){Icon(Icons.Default.ArrowBack,"Volver")}
    }
   },
   title={Text(selected?.name ?: "Índice temático")}
  )},
  floatingActionButton={FloatingActionButton(onClick={if(selected==null) newTopic=true else newResource=true}){Icon(Icons.Default.Add,"Añadir")}}
 ){pad->
  Column(Modifier.padding(pad).padding(16.dp)){
   if(selected==null){
    OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),label={Text("Buscar temas")},singleLine=true)
    Spacer(Modifier.height(12.dp))
    if(topics.isEmpty()) Text(if(query.isBlank())"Crea tu primer tema con +." else "No hay temas que coincidan.")
    LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
     items(topics,key={it.id}){t->
      Card(onClick={selected=t;query=""},modifier=Modifier.fillMaxWidth()){
       Column(Modifier.padding(18.dp)){Text(t.name,style=MaterialTheme.typography.titleMedium);if(t.description.isNotBlank())Text(t.description)}
      }
     }
    }
   } else TopicScreen(selected!!,dao)
  }
 }

 if(newTopic) TopicDialog(onDismiss={newTopic=false}){name,desc->scope.launch{dao.insertTopic(Topic(name=name,description=desc))};newTopic=false}
 if(newResource) ResourceDialog(onDismiss={newResource=false}){r->scope.launch{dao.insertResource(r.copy(topicId=selected!!.id))};newResource=false}
}

@Composable fun TopicScreen(topic:Topic,dao:NoteDao){
 val resources by dao.resources(topic.id).collectAsState(initial=emptyList())
 val scope=rememberCoroutineScope()
 if(resources.isEmpty()){Text("Todavía no hay citas ni recursos en este tema. Pulsa + para añadir uno.");return}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
  items(resources,key={it.id}){r->ResourceCard(r,
   onFavorite={scope.launch{dao.updateResource(r.copy(favorite=!r.favorite,updatedAt=System.currentTimeMillis()))}},
   onDelete={scope.launch{dao.deleteResource(r)}}
  )}
 }
}

@Composable fun ResourceCard(r:Resource,onFavorite:()->Unit,onDelete:()->Unit){
 var expanded by remember(r.id){mutableStateOf(false)}
 Card(onClick={expanded=!expanded},modifier=Modifier.fillMaxWidth()){
  Column(Modifier.padding(16.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
    Column(Modifier.weight(1f)){Text(r.reference,style=MaterialTheme.typography.titleMedium);Text(r.type,style=MaterialTheme.typography.labelMedium)}
    IconButton(onClick=onFavorite){Icon(if(r.favorite)Icons.Default.Star else Icons.Outlined.StarBorder,"Favorito")}
   }
   AnimatedVisibility(expanded){
    Column{
     HorizontalDivider(Modifier.padding(vertical=10.dp))
     if(r.text.isNotBlank())Text(r.text)
     if(r.source.isNotBlank()){Spacer(Modifier.height(6.dp));Text("Fuente: "+r.source,style=MaterialTheme.typography.labelMedium)}
     if(r.personalNote.isNotBlank()){Spacer(Modifier.height(6.dp));Text("Nota: "+r.personalNote)}
     TextButton(onClick=onDelete){Text("Eliminar")}
    }
   }
  }
 }
}

@Composable fun TopicDialog(onDismiss:()->Unit,onSave:(String,String)->Unit){
 var name by remember{mutableStateOf("")};var desc by remember{mutableStateOf("")}
 AlertDialog(onDismissRequest=onDismiss,title={Text("Nuevo tema")},text={Column{OutlinedTextField(name,{name=it},label={Text("Tema")});OutlinedTextField(desc,{desc=it},label={Text("Descripción opcional")})}},
  confirmButton={Button(enabled=name.isNotBlank(),onClick={onSave(name.trim(),desc.trim())}){Text("Crear")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})
}

@Composable fun ResourceDialog(onDismiss:()->Unit,onSave:(Resource)->Unit){
 var type by remember{mutableStateOf("Biblia")};var ref by remember{mutableStateOf("")};var body by remember{mutableStateOf("")};var source by remember{mutableStateOf("")};var note by remember{mutableStateOf("")}
 val types=listOf("Biblia","Santo / Magisterio","Libro","Pensamiento","Nota propia")
 AlertDialog(onDismissRequest=onDismiss,title={Text("Añadir cita o recurso")},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
  Text("Tipo",style=MaterialTheme.typography.labelLarge)
  Row{types.take(3).forEach{FilterChip(selected=type==it,onClick={type=it},label={Text(it)})}}
  Row{types.drop(3).forEach{FilterChip(selected=type==it,onClick={type=it},label={Text(it)})}}
  OutlinedTextField(ref,{ref=it},label={Text("Referencia o título · ej. Mt 5,5")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(body,{body=it},label={Text("Texto / descripción")},modifier=Modifier.fillMaxWidth(),minLines=3)
  OutlinedTextField(source,{source=it},label={Text("Fuente / autor")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(note,{note=it},label={Text("Nota personal")},modifier=Modifier.fillMaxWidth())
 }},confirmButton={Button(enabled=ref.isNotBlank(),onClick={onSave(Resource(topicId=0,type=type,reference=ref.trim(),text=body.trim(),source=source.trim(),personalNote=note.trim()))}){Text("Guardar")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})
}
