package com.example.tatratracker.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Udostępnianie obrazka z podsumowaniem wędrówki. Obrazek trafia do pamięci podręcznej
 * aplikacji (`cache/shared`, jeden plik nadpisywany przy każdym udostępnieniu) i jest
 * wystawiany innym aplikacjom przez FileProvider – bez żadnych uprawnień do plików.
 */
object SummaryImageSharer {

    // Muszą zgadzać się z AndroidManifest.xml i res/xml/shared_files.xml.
    private const val AUTHORITY_SUFFIX = ".fileprovider"
    private const val SHARED_DIRECTORY = "shared"

    private const val FILE_NAME = "tatratracker-podsumowanie.png"
    private const val MIME_TYPE = "image/png"

    /** Zapisuje obrazek jako PNG i zwraca adres, który można przekazać innej aplikacji. */
    suspend fun save(context: Context, bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, SHARED_DIRECTORY)

        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Nie udało się utworzyć katalogu na obrazek.")
        }

        val file = File(directory, FILE_NAME)

        // Bitmapy sprzętowej (tak oddaje ją Compose) nie da się wprost zapisać na każdym Androidzie.
        val writable =
            if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
                    ?: throw IOException("Nie udało się przygotować obrazka.")
            } else {
                bitmap
            }

        file.outputStream().use { output ->
            if (!writable.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw IOException("Nie udało się zapisać obrazka.")
            }
        }

        FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
    }

    /** Systemowe okno „Udostępnij” z obrazkiem i krótkim opisem. */
    fun chooserIntent(imageUri: Uri, text: String): Intent {
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, text)
                // Podgląd obrazka w oknie udostępniania i prawo odczytu dla wybranej aplikacji.
                clipData = ClipData.newRawUri(null, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

        return Intent.createChooser(send, "Udostępnij podsumowanie").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
