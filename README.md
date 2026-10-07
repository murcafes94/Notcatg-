# Notcatg — Índice temático personal

Aplicación Android nativa en Kotlin, Jetpack Compose y Room. Organiza citas y notas para estudiar, responder preguntas y preparar charlas. Los datos se guardan en el dispositivo; no necesita una cuenta ni conexión.

## Uso

1. Pulsa **+** para crear un tema, por ejemplo «Jóvenes en problemas con la fe».
2. Abre el tema y pulsa **+** para añadir Biblia, Santo / Magisterio, Libro, Pensamiento o Nota propia.
3. Guarda una referencia o título, el texto, la fuente y tus observaciones. Toca la referencia para desplegar el contenido.
4. Usa **Editar** dentro de la cita o **Editar tema**. Los cambios mantienen el identificador, la fecha de creación y los favoritos.
5. Busca por referencia, contenido, autor o nota personal; la búsqueda ignora mayúsculas y tildes. Combina tipo y favoritos.
6. Las eliminaciones requieren confirmación. Eliminar un tema borra sus recursos en una sola transacción.

## Biblia de Navarra sin conexión

La consulta web se ha retirado. En un recurso de tipo **Biblia**, pulsa **Importar EPUB de Navarra** y selecciona una vez el archivo `Sagrada Biblia [Castellano] (Ed. Univ. de Navarra).epub`. La app prepara un índice SQLite local con los 73 libros; no abre páginas web ni requiere internet para buscar. La importación se hace en segundo plano y una nueva importación solo sustituye el índice bíblico si se ha validado el archivo completo. La colección personal utiliza otra base y no se borra.

Escribe `Mt 5,5`, `1 Co 13,4-7` o `Est 4,17a` y pulsa **Buscar cita en Navarra**. Se admite un máximo de 20 versículos del mismo capítulo y referencias individuales con letras. Se conserva la numeración que aparece en Navarra, incluidas sus alternativas y pasajes impresos con números conjuntos. Se recupera el texto bíblico, sin introducir encabezados, números de capítulo ni comentarios editoriales. Pulsa **Guardar** para conservarlo junto con la atribución dentro del tema. No sobrescribe el texto que ya escribiste.

El EPUB original se importa en el dispositivo del usuario; no se incluye en el repositorio público ni en el APK. Mantén una copia del EPUB para volver a importar la Biblia al cambiar de dispositivo. Los respaldos de tus notas incluyen el texto de las citas ya guardadas, aunque el EPUB no esté importado en el nuevo dispositivo. La importación es específica del formato revisado de Navarra, con límite de 25 MB; otros EPUB pueden no ser compatibles.

La revisión local del EPUB proporcionado identificó 73 libros y 35.784 referencias, y comprobó referencias de Génesis, Mateo, 1 Corintios, Ester y Apocalipsis. Las pruebas del importador cubren continuación de párrafos, poesía, exclusión de encabezados, numeraciones alternativas y versículos con letras. Está pendiente comprobar la importación y el uso completo en Android real.

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
