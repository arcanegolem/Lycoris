package ru.spektrit.lycoris.viewdocument

import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.http.Url
import ru.spektrit.lycoris.network.provideDownloadInterface
import ru.spektrit.lycoris.utils.PfdHelper
import ru.spektrit.lycoris.utils.provideFileName
import java.io.File


/**
 * PDF document viewer (via [LazyColumn]) from [RawRes]
 *
 * @param [modifier] [Modifier]
 * @param [pdfResId] Raw resource id of a PDF document
 * @param [verticalArrangement] Page arrangement
 */
@Composable
fun PdfViewer(
   modifier: Modifier = Modifier,
   @RawRes pdfResId: Int,
   verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
) {
   val documentIdentifier = pdfResId.toString()

   val rendererScope = rememberCoroutineScope()
   val mutex = remember { Mutex() }

   val context = LocalContext.current

   val renderer by produceState<PdfRenderer?>(null, null) {
      rendererScope.launch(Dispatchers.IO) {
         val pfdHelper = PfdHelper()
         value = PdfRenderer(pfdHelper.getPfd(context, pdfResId, documentIdentifier))
      }
      awaitDispose {
         val currentRenderer = value
         rendererScope.launch(Dispatchers.IO) {
            mutex.withLock {
               currentRenderer?.close()
            }
         }
      }
   }

   PdfViewerDisplay(
      modifier = modifier,
      context = context,
      renderer = renderer,
      documentIdentifier = documentIdentifier,
      verticalArrangement = verticalArrangement,
      mutex = mutex
   )
}


/**
 * PDF document viewer (via [LazyColumn]) from [Uri]
 *
 * @param [modifier] [Modifier] interface for the whole composable, by default fills max size available
 * @param [uri] [Uri] from which document should be retrieved
 * @param [verticalArrangement] Page arrangement
 */
@Composable
fun PdfViewer(
   modifier: Modifier = Modifier,
   uri: Uri,
   verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
) {
   val rendererScope = rememberCoroutineScope()
   val mutex = remember { Mutex() }

   val context = LocalContext.current

   val renderer by produceState<PdfRenderer?>(null, uri) {
      rendererScope.launch(Dispatchers.IO) {
         val pfdHelper = PfdHelper()
         value = PdfRenderer(pfdHelper.getPfd(uri))
      }
      awaitDispose {
         val currentRenderer = value
         rendererScope.launch(Dispatchers.IO) {
            mutex.withLock { currentRenderer?.close() }
         }
      }
   }

   PdfViewerDisplay(
      modifier = modifier,
      context = context,
      renderer = renderer,
      documentIdentifier = uri.toString(),
      verticalArrangement = verticalArrangement,
      mutex = mutex
   )
}


/**
 * PDF document viewer (via [LazyColumn]) from [Url]
 *
 * @param [modifier] [Modifier] interface for the whole composable, by default fills max size available
 * @param [url] [Url] where PDF document is stored at
 * @param [headers] optional http headers
 * @param [verticalArrangement] Page arrangement
 */
@Composable
fun PdfViewer(
   modifier: Modifier = Modifier,
   @Url url: String,
   headers: HashMap<String, String> = hashMapOf( "headerKey" to "headerValue" ),
   verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
) {

   val rendererScope = rememberCoroutineScope()
   val mutex = remember { Mutex() }

   var docLoadPercentage by remember { mutableIntStateOf( 0 ) }
   var docLoad = 0
   val bufferSize = 8192

   val context = LocalContext.current

   // TODO: Возможно стоит убрать постоянное кэширование при загрузке
   // TODO: Сделать файлы временными как в случае с @RawRes (???)
   val renderer by produceState<PdfRenderer?>(null, null) {
      rendererScope.launch(Dispatchers.IO) {
         val file = File(context.cacheDir, provideFileName())
         val response = provideDownloadInterface(headers).downloadFile(url)
         val responseByteStream = response.byteStream()

         responseByteStream.use { inputStream ->
            file.outputStream().use { outputStream ->
               val totalByteCount = response.contentLength()
               var data = ByteArray(bufferSize)
               var count = inputStream.read(data)
               while (count != -1) {
                  if (totalByteCount > 0) {
                     docLoad += bufferSize
                     docLoadPercentage = (docLoad * (100 / totalByteCount.toFloat())).toInt()
                  }
                  outputStream.write(data, 0, count)
                  data = ByteArray(bufferSize)
                  count = inputStream.read(data)
               }
            }
         }

         value = PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
      }
      awaitDispose {
         val currentRenderer = value
         rendererScope.launch(Dispatchers.IO) { mutex.withLock { currentRenderer?.close() } }
      }
   }

   PdfViewerDisplay(
      modifier = modifier,
      context = context,
      renderer = renderer,
      documentIdentifier = url,
      verticalArrangement = verticalArrangement,
      mutex = mutex
   )
}



