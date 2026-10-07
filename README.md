# Notcatg — Índice temático personal

Aplicación Android nativa en Kotlin, Jetpack Compose y Room. Organiza citas y notas para estudiar, responder preguntas y preparar charlas. Los datos se guardan en el dispositivo; no necesita una cuenta ni conexión.

## Uso

1. Pulsa **+** para crear un tema, por ejemplo «Jóvenes en problemas con la fe».
2. Abre el tema y pulsa **+** para añadir Biblia, Santo / Magisterio, Libro, Pensamiento o Nota propia.
3. Guarda una referencia o título, el texto, la fuente y tus observaciones. Toca la referencia para desplegar el contenido.
4. Usa **Editar** dentro de la cita o **Editar tema**. Los cambios mantienen el identificador, la fecha de creación y los favoritos.
5. Busca por referencia, contenido, autor o nota personal; la búsqueda ignora mayúsculas y tildes. Combina tipo y favoritos.
6. Las eliminaciones requieren confirmación. Eliminar un tema borra sus recursos en una sola transacción.

El texto bíblico se introduce manualmente: no hay descarga automática ni una traducción bíblica incorporada. No se incluyen citas de ejemplo atribuidas sin verificar.

## Compilación

Android Studio con JDK 17, SDK 35 y Gradle 8.11.1. Si tienes Gradle instalado:

```sh
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

GitHub Actions ejecuta estas verificaciones en `main` y en las solicitudes de cambios. En una ejecución exitosa, descarga **apk-notcatg-debug** en Artifacts y extrae `app-debug.apk`. Es una compilación de prueba, no una versión firmada para publicación.

## Datos y límites actuales

La base actual es `notcatg.db`, versión 2, con tablas `topics` y `resources`. Este cambio mantiene ese esquema y elimina la opción de reiniciarlo automáticamente ante una migración ausente. La antigua aplicación de apuntes utilizaba `notes.db`; su importación no está implementada y ese archivo no se borra. Aún faltan exportación/importación de copias y sincronización entre dispositivos.

Las pruebas unitarias cubren la búsqueda por los distintos campos y la combinación de filtros. Compilación y lint no sustituyen una prueba de uso en un teléfono o tablet.
