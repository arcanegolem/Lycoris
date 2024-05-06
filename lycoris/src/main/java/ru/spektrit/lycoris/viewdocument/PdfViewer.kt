package ru.spektrit.lycoris.viewdocument

import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.http.Url
import ru.spektrit.lycoris.network.provideDownloadInterface
import ru.spektrit.lycoris.utils.PfdHelper


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
   iconTint : Color = Color.Black,
   accentColor : Color = Color.DarkGray,
   controlsAlignment: Alignment = Alignment.BottomEnd,
   bitmapScale : Int = 1
) {
   val documentIdentifier = pdfResId.toString()

   val rendererScope = rememberCoroutineScope()
   val mutex = remember { Mutex() }

   val context = LocalContext.current

   val renderer by produceState<PdfRenderer?>(null, pdfResId) {
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
      accentColor = accentColor,
      modifier = modifier,
      context = context,
      renderer = renderer,
      documentIdentifier = documentIdentifier,
      verticalArrangement = verticalArrangement,
      mutex = mutex,
      controlsAlignment = controlsAlignment,
      iconTint = iconTint,
      bitmapScale = bitmapScale
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
   accentColor: Color = Color.DarkGray,
   iconTint: Color = Color.Black,
   controlsAlignment: Alignment = Alignment.BottomEnd,
   bitmapScale : Int = 1
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
      accentColor = accentColor,
      modifier = modifier,
      context = context,
      renderer = renderer,
      documentIdentifier = uri.toString(),
      verticalArrangement = verticalArrangement,
      mutex = mutex,
      controlsAlignment = controlsAlignment,
      iconTint = iconTint,
      bitmapScale = bitmapScale
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
   headers: HashMap<String, String>? = null,
   verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
   accentColor: Color = Color.DarkGray,
   iconTint: Color = Color.Black,
   controlsAlignment: Alignment = Alignment.BottomEnd,
   bitmapScale : Int = 1
) {

   val rendererScope = rememberCoroutineScope()
   val mutex = remember { Mutex() }

   val context = LocalContext.current

   val renderer by produceState<PdfRenderer?>(null, url) {
      rendererScope.launch(Dispatchers.IO) {
         val pfdHelper = PfdHelper()

         value = PdfRenderer(pfdHelper.getPfd(url.split("/").last(), context, provideDownloadInterface(headers).downloadFile(url)))
      }
      awaitDispose {
         val currentRenderer = value
         rendererScope.launch(Dispatchers.IO) { mutex.withLock { currentRenderer?.close() } }
      }
   }

   PdfViewerDisplay(
      accentColor = accentColor,
      modifier = modifier,
      context = context,
      renderer = renderer,
      documentIdentifier = url,
      verticalArrangement = verticalArrangement,
      mutex = mutex,
      controlsAlignment = controlsAlignment,
      iconTint = iconTint,
      bitmapScale = bitmapScale
   )
}



