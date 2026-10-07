# Notcatg — Índice temático personal

Aplicación Android nativa en Kotlin, Jetpack Compose y Room. Organiza citas y notas para estudiar, responder preguntas y preparar charlas. Los datos se guardan en el dispositivo; no necesita una cuenta ni conexión.

## Uso

1. Pulsa **+** para crear un tema, por ejemplo «Jóvenes en problemas con la fe».
2. Abre el tema y pulsa **+** para añadir Biblia, Santo / Magisterio, Libro, Pensamiento o Nota propia.
3. Guarda una referencia o título, el texto, la fuente y tus observaciones. Toca la referencia para desplegar el contenido.
4. Usa **Editar** dentro de la cita o **Editar tema**. Los cambios mantienen el identificador, la fecha de creación y los favoritos.
5. Busca por referencia, contenido, autor o nota personal; la búsqueda ignora mayúsculas y tildes. Combina tipo y favoritos.
6. Las eliminaciones requieren confirmación. Eliminar un tema borra sus recursos en una sola transacción.

## Citas bíblicas online

En un recurso de tipo **Biblia**, escribe una referencia como `Mt 5,4` o `1 Co 13,4-7` y pulsa **Buscar texto online**. Notcatg abre el buscador público de https://bdj.alpichel.com/libros y recupera los versículos solicitados. Utiliza el buscador público de la página, sin una API privada. Se admiten citas de un capítulo, con un máximo de 20 versículos.

La respuesta debe coincidir en libro y capítulo y contener todos los versículos solicitados. Si la consulta falla, no se guarda texto incompleto ni se sustituye por otra traducción. El formulario permite revisar el contenido y pulsar **Guardar**: texto y atribución se conservan en Room y se leen después sin internet. El botón online se desactiva si ya hay texto, para evitar sobrescribir una cita manual.

La numeración es la del proveedor: en la edición consultada, «Bienaventurados los mansos» es Mt 5,4. La página se presenta como Biblia de Jerusalén; no hemos verificado qué edición impresa específica utiliza. La consulta depende de su buscador y del WebView de Android; puede fallar si el sitio cambia o deja de responder. Hay un tiempo máximo de 30 segundos y opción de cerrar. La consulta online requiere internet; la consulta de recursos guardados no.

La prueba del buscador se realizó en navegador. Está pendiente comprobar el flujo completo en un dispositivo Android real.

## Compilación

Android Studio con JDK 17, SDK 35 y Gradle 8.11.1. Si tienes Gradle instalado:

```sh
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

GitHub Actions ejecuta estas verificaciones en `main` y en las solicitudes de cambios. En una ejecución exitosa, descarga **apk-notcatg-debug** en Artifacts y extrae `app-debug.apk`. Es una compilación de prueba, no una versión firmada para publicación.

## Datos y límites actuales

La base actual es `notcatg.db`, versión 2, con tablas `topics` y `resources`. Este cambio mantiene ese esquema y elimina la opción de reiniciarlo automáticamente ante una migración ausente. La antigua aplicación de apuntes utilizaba `notes.db`; su importación no está implementada y ese archivo no se borra. La sincronización automática entre dispositivos aún no está implementada.

Las pruebas unitarias cubren la búsqueda por los distintos campos y la combinación de filtros. Compilación y lint no sustituyen una prueba de uso en un teléfono o tablet.

## Respaldos en Google Drive

Pulsa el icono de ajustes en la barra superior y elige **Guardar respaldo**. En el selector de archivos de Android, abre el menú y selecciona **Drive** (requiere la aplicación de Google Drive instalada y una cuenta configurada), o una carpeta local. El archivo JSON incluye temas, textos, fuentes, notas personales, favoritos y fechas. Este respaldo es manual, no una sincronización automática. Android entrega el archivo al proveedor de Drive; la subida puede terminar después, por lo que conviene verificar que aparezca allí.

Para recuperar un respaldo, pulsa **Abrir respaldo**, selecciona el JSON en Drive y revisa la cantidad de temas y recursos antes de pulsar **Restaurar**. La restauración combina el respaldo con los datos actuales en una transacción: no borra ni sobrescribe registros. Omite recursos idénticos, incluidos los de una segunda restauración del mismo archivo. Si un tema o recurso cambió desde el respaldo, conserva ambas versiones.

Solo se aceptan respaldos de Notcatg de hasta 5 MB y 20.000 registros con versión y relaciones válidas. El archivo contiene tus notas en texto legible; guárdalo en tu propio Drive. No se necesitan credenciales de Google dentro de Notcatg ni permisos de acceso general al almacenamiento. Está pendiente probar el selector y la subida con Drive en un dispositivo real.
