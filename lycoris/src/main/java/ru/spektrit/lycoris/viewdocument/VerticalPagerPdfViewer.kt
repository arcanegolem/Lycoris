package ru.spektrit.lycoris.viewdocument

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RawRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import retrofit2.http.Url
import ru.spektrit.lycoris.network.provideDownloadInterface
import ru.spektrit.lycoris.utils.PfdHelper
import ru.spektrit.lycoris.utils.provideFileName
import java.io.File
import kotlin.math.sqrt

/**
 * PDF document viewer (via [VerticalPager]) from [RawRes]"
 *
 * @param [modifier] [Modifier] interface for the whole composable, by default fills max size available
 * @param [pdfResId] Raw resource id of a PDF document
 * @param [documentDescription] Similar to content description but cannot be null in this case
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalPagerPdfViewer(
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

         val pagerState = rememberPagerState(pageCount = { r.pageCount })
         val imageLoadingScope = rememberCoroutineScope()
         val imageLoader = context.imageLoader

         VerticalPager(state = pagerState) { index ->
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

/**
 * PDF document viewer (via [VerticalPager]) from [Uri]"
 *
 * @param [modifier] [Modifier] interface for the whole composable, by default fills max size available
 * @param [uri] [Uri] from which document should be retrieved
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalPagerPdfViewer(
   modifier: Modifier = Modifier.fillMaxWidth(),
   uri: Uri,
) {
   val rendererScope = rememberCoroutineScope()
   val mutex = remember { Mutex() }

   val context = LocalContext.current

   val renderer by produceState<PdfRenderer?>(null, null) {
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

   renderer?.let { r ->
      BoxWithConstraints(modifier = modifier) {
         val width = with(LocalDensity.current) { maxWidth.toPx() }.toInt()
         val height = (width * sqrt(2f)).toInt()

         val pagerState = rememberPagerState(pageCount = { r.pageCount })
         val imageLoadingScope = rememberCoroutineScope()
         val imageLoader = context.imageLoader

         VerticalPager(state = pagerState) { index ->
            val cacheKey = MemoryCache.Key("$uri - $index")
            val cacheValue : Bitmap? = imageLoader.memoryCache?.get(cacheKey)?.bitmap

            var bitmap : Bitmap? by remember { mutableStateOf(cacheValue) }

            if (bitmap == null) {
               DisposableEffect(uri, index) {
                  val job = imageLoadingScope.launch(Dispatchers.IO) {
                     val destinationBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                     mutex.withLock {
                        Log.d("PdfGenerator", "Loading PDF $uri - page ${index + 1}/${r.pageCount}")

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

/**
 * PDF document viewer (via [VerticalPager]) from [Url]
 *
 * @param [modifier] [Modifier] interface for the whole composable, by default fills max size available
 * @param [url] [Url] where PDF document is stored at
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalPagerPdfViewer(
   modifier: Modifier = Modifier.fillMaxWidth(),
   @Url url: String,
   headers: HashMap<String, String>,
) {
   val rendererScope = rememberCoroutineScope()
   val mutex = remember { Mutex() }

   val bufferSize = 8192

   val context = LocalContext.current

   val renderer by produceState<PdfRenderer?>(null, null) {
      rendererScope.launch(Dispatchers.IO) {
         val file = File(context.cacheDir, provideFileName())
         val response = provideDownloadInterface(headers).downloadFile(url)
         val responseByteStream = response.byteStream()

         responseByteStream.use { inputStream ->
            file.outputStream().use { outputStream ->
               var data = ByteArray(bufferSize)
               var count = inputStream.read(data)
               while (count != -1) {
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
         rendererScope.launch(Dispatchers.IO) {
            mutex.withLock { currentRenderer?.close() }
         }
      }
   }

   renderer?.let { r ->
      BoxWithConstraints(modifier = modifier) {
         val width = with(LocalDensity.current) { maxWidth.toPx() }.toInt()
         val height = (width * sqrt(2f)).toInt()

         val pagerState = rememberPagerState(pageCount = { r.pageCount })
         val imageLoadingScope = rememberCoroutineScope()
         val imageLoader = context.imageLoader

         VerticalPager(state = pagerState) { index ->
            val cacheKey = MemoryCache.Key("$url - $index")
            val cacheValue : Bitmap? = imageLoader.memoryCache?.get(cacheKey)?.bitmap

            var bitmap : Bitmap? by remember { mutableStateOf(cacheValue) }

            if (bitmap == null) {
               DisposableEffect(url, index) {
                  val job = imageLoadingScope.launch(Dispatchers.IO) {
                     val destinationBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                     mutex.withLock {
                        Log.d("PdfGenerator", "Loading PDF $url - page ${index + 1}/${r.pageCount}")

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