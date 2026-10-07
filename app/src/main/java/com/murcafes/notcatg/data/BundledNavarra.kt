package com.murcafes.notcatg.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Index the personal APK's bundled EPUB once, without touching the user's notes. */
object BundledNavarra {
    private val mutex = Mutex()

    suspend fun prepare(context: Context) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val dao = NavarraDatabase.get(context).verses()
            if (dao.total() > 0) return@withLock
            val file = File.createTempFile("navarra-bundled-", ".epub", context.cacheDir)
            try {
                context.assets.open("navarra.epub").use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytes = 0L
                        while (true) {
                            val size = input.read(buffer)
                            if (size == -1) break
                            bytes += size
                            require(bytes <= NavarraEpub.MAX_EPUB_BYTES)
                            output.write(buffer, 0, size)
                        }
                    }
                }
                // Parse and validate fully before the atomic insert. Interrupted setup can retry.
                dao.replaceBible(NavarraEpub.parse(file))
            } finally {
                file.delete()
            }
        }
    }
}
