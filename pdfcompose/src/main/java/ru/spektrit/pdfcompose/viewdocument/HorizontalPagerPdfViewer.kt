package ru.spektrit.pdfcompose.viewdocument

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.util.Log
import androidx.annotation.RawRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.spektrit.pdfcompose.utils.PfdHelper
import kotlin.math.sqrt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalPagerPdfViewer(
   modifier: Modifier = Modifier.fillMaxWidth(),
   @RawRes pdfResId: Int,
   documentDescription : String
) {
   val rendererScope = rememberCoroutineScope()
   val mutex = remember { Mutex() }

   val context = LocalContext.current

   val renderer by produceState<PdfRenderer?>(null, null) {
      rendererScope.launch(Dispatchers.IO) {
         val pfdHelper = PfdHelper()
         value = PdfRenderer(pfdHelper.getPfd(context, pdfResId, documentDescription))
      }
      awaitDispose {
         val currentRenderer = value
         rendererScope.launch(Dispatchers.IO) {
            mutex.withLock { currentRenderer?.close() }
         }
      }
   }

   renderer?.let { r ->
      BoxWithConstraints(modifier = modifier) {
         val width = with(LocalDensity.current) { maxWidth.toPx() }.toInt()
         val height = (width * sqrt(2f)).toInt()

         val imageLoadingScope = rememberCoroutineScope()
         val imageLoader = context.imageLoader

         HorizontalPager(pageCount = r.pageCount) { index ->
            val cacheKey = MemoryCache.Key("$documentDescription - $index")
            val cacheValue : Bitmap? = imageLoader.memoryCache?.get(cacheKey)?.bitmap

            var bitmap : Bitmap? by remember { mutableStateOf(cacheValue) }

            if (bitmap == null) {
               DisposableEffect(documentDescription, index) {
                  val job = imageLoadingScope.launch(Dispatchers.IO) {
                     val destinationBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                     mutex.withLock {
                        Log.d("PdfGenerator", "Loading PDF $documentDescription - page ${index + 1}/${r.pageCount}")

                        if (!coroutineContext.isActive) return@launch

                        try {
                           r.let{
                              it.openPage(index).use { rPage ->
                                 rPage.render(destinationBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                              }
                           }
                        } catch (e: Exception) { return@launch }
                     }
                     bitmap = destinationBitmap
                  }
                  onDispose { job.cancel() }
               }
               PagePlaceHolder(backgroundColor = Color.White)
            } else {
               val request = ImageRequest.Builder(context)
                  .size(width, height)
                  .memoryCacheKey(cacheKey)
                  .data(bitmap)
                  .build()

               Box {
                  Image(
                     modifier = Modifier
                        .aspectRatio(1f / sqrt(2f))
                        .fillMaxWidth(),
                     painter = rememberAsyncImagePainter(request),
                     contentDescription = "Page ${index + 1} of ${r.pageCount}"
                  )
               }
            }
         }
      }
   }
}