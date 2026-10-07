package com.murcafes.notcatg

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.murcafes.notcatg.data.BibleReference
import com.murcafes.notcatg.data.BibleVerse
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BibleLookupDialog(reference: BibleReference, onDismiss: () -> Unit, onResult: (String, String) -> Unit) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var status by remember { mutableStateOf("Consultando Biblia de Jerusalén…") }
    var finished by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }
    var attempts by remember { mutableIntStateOf(0) }
    // No JavaScript bridge: only read the public search result labels and text.
    val script = remember(reference) {
        """(function(){
          const input=document.querySelector('input.search-input');
          if(!input) return null;
          const query=${JSONObject.quote(reference.query)};
          if(input.value!==query){
            const setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;
            setter.call(input,query);
            input.dispatchEvent(new Event('input',{bubbles:true}));
            input.dispatchEvent(new Event('change',{bubbles:true}));
            return null;
          }
          const rows=Array.from(document.querySelectorAll('main button.book-row.result'));
          return JSON.stringify(rows.map(row=>({label:row.querySelector('b')?.textContent||'',text:row.querySelector('small')?.textContent||''})));
        })()""".trimIndent()
    }
    LaunchedEffect(webView) {
        while (!finished && attempts < 40) {
            delay(750)
            val view = webView ?: continue
            attempts++
            if (view.url?.startsWith("https://bdj.alpichel.com/libros") != true) continue
            started = true
            view.evaluateJavascript(script) { raw ->
                if (!finished) {
                    try {
                        val decoded = JSONTokener(raw).nextValue() as? String
                        if (decoded != null) {
                            val rows = JSONArray(decoded)
                            val verses = (0 until rows.length()).map { index ->
                                val row = rows.getJSONObject(index)
                                BibleVerse(row.getString("label"), row.getString("text"))
                            }
                            reference.extract(verses)?.let { text ->
                                finished = true
                                onResult(text, "Biblia de Jerusalén · bdj.alpichel.com · ${reference.query}\nhttps://bdj.alpichel.com/libros")
                            }
                        }
                    } catch (_: Exception) {
                        // Incomplete loading or changed provider markup: retry until the bounded timeout.
                    }
                }
            }
        }
        if (!finished) {
            finished = true
            status = if (started) "No se pudo recuperar la cita completa. Revisa la referencia o ingresa el texto manualmente."
                else "No se pudo conectar. Puedes guardar la referencia y añadir el texto cuando tengas internet."
        }
    }
    DisposableEffect(Unit) {
        onDispose { finished = true; webView?.stopLoading(); webView?.destroy(); webView = null }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Buscar cita online") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(status)
                if (!finished) LinearProgressIndicator(Modifier.fillMaxWidth())
                AndroidView(modifier = Modifier.fillMaxWidth().height(220.dp), factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                                request.url.scheme != "https" || request.url.host != "bdj.alpichel.com"
                        }
                        webView = this
                        loadUrl("https://bdj.alpichel.com/libros")
                    }
                })
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}
