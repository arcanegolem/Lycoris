package ru.spektrit.pdfcompose.viewdocument

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toFile
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.spektrit.pdfcompose.utils.ImageDialog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.sqrt

/**
 * PDF document viewer from resources
 *
 * [modifier] - [Modifier] interface for the whole composable, by default fills max size available
 * [pdfResId] - Raw resource id of a PDF document
 * [documentDescription] - Similar to content description but cannot be null in this case
 * [verticalArrangement] - Page arrangement
 */
@Composable
fun PdfViewer(
   modifier: Modifier = Modifier.fillMaxSize(),
   @RawRes pdfResId: Int,
   documentDescription: String,
   verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp)
) {
   val rendererScope = rememberCoroutineScope()
   val mutex = remember { Mutex() }

   val context = LocalContext.current

   val renderer by produceState<PdfRenderer?>(null, null) {
      rendererScope.launch(Dispatchers.IO) {
         val inputStream = context.resources.openRawResource(pdfResId)
         val outputDir = context.cacheDir
         val tempFile = File.createTempFile(documentDescription, "pdf", outputDir)
         tempFile.mkdirs()
         tempFile.deleteOnExit()
         val outputStream = FileOutputStream(tempFile)
         copy(inputStream, outputStream)
         val input = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
         value = PdfRenderer(input)
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
   val imageLoader = context.imageLoader
   val imageLoadingScope = rememberCoroutineScope()
   BoxWithConstraints(modifier = modifier) {
      val width = with(LocalDensity.current) { maxWidth.toPx() }.toInt()
      val height = (width * sqrt(2f)).toInt()
      val pageCount by remember(renderer) { derivedStateOf { renderer?.pageCount ?: 0 } }

      val lazyListState = rememberLazyListState()
      var currentPage by remember { mutableStateOf(1) }

      // TODO: Оптимизировать отслеживание текущей страницы
      val fullyVisibleIndices: List<Int> by remember {
         derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
               emptyList()
            } else {
               val fullyVisibleItemsInfo = visibleItemsInfo.toMutableList()
               val lastItem = fullyVisibleItemsInfo.last()
               val viewportHeight = layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset
               if (lastItem.offset + lastItem.size > viewportHeight) { fullyVisibleItemsInfo.removeLast() }
               val firstItemIfLeft = fullyVisibleItemsInfo.firstOrNull()
               if (firstItemIfLeft != null && firstItemIfLeft.offset < layoutInfo.viewportStartOffset) {
                  fullyVisibleItemsInfo.removeFirst()
               }

               fullyVisibleItemsInfo.map { it.index }
            }
         }
      }

      LazyColumn(
         verticalArrangement = verticalArrangement,
         state = lazyListState
      ) {
         items(
            count = pageCount,
            key = { index -> "$documentDescription-$index" }
         ) { index ->
            val isVisible by remember(index) {
               derivedStateOf { fullyVisibleIndices.contains(index) } }

            if (isVisible) { currentPage = index }

            val cacheKey = MemoryCache.Key("$documentDescription-$index")
            val cacheValue : Bitmap? = imageLoader.memoryCache?.get(cacheKey)?.bitmap

            var bitmap : Bitmap? by remember { mutableStateOf(cacheValue)}
            if (bitmap == null) {
               DisposableEffect(documentDescription, index) {
                  val job = imageLoadingScope.launch(Dispatchers.IO) {
                     val destinationBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                     mutex.withLock {
                        Log.d("PdfGenerator", "Loading PDF $documentDescription - page ${index + 1}/$pageCount")
                        if (!coroutineContext.isActive) return@launch
                        try {
                           renderer?.let {
                              it.openPage(index).use { page ->
                                 page.render(destinationBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                              }
                           }
                        } catch (e: Exception) {
                           return@launch
                        }
                     }
                     bitmap = destinationBitmap
                  }
                  onDispose {
                     job.cancel()
                  }
               }
               Box(modifier = Modifier
                  .background(Color.White)
                  .aspectRatio(1f / sqrt(2f))
                  .fillMaxWidth())
            } else {
               val request = ImageRequest.Builder(context)
                  .size(width, height)
                  .memoryCacheKey(cacheKey)
                  .data(bitmap)
                  .build()

               Box {
                  var isEnlargedDialogDisplayed by remember { mutableStateOf(false) }
                  val interactionSource = MutableInteractionSource()

                  Icon(
                     modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(15.dp)
                        .zIndex(1f),
                     imageVector = Icons.Rounded.Search,
                     contentDescription = "Magnify Indicator"
                  )

                  Image(
                     modifier = Modifier
                        .background(Color.White)
                        .aspectRatio(1f / sqrt(2f))
                        .fillMaxWidth()
                        .clickable(
                           interactionSource = interactionSource,
                           indication = null
                        ) { isEnlargedDialogDisplayed = true },
                     contentScale = ContentScale.Fit,
                     painter = rememberAsyncImagePainter(request),
                     contentDescription = "Page ${index + 1} of $pageCount"
                  )

                  if (isEnlargedDialogDisplayed) {
                     ImageDialog(img = request) {
                        isEnlargedDialogDisplayed = false
                     }
                  }
               }
            }

            if (index != pageCount - 1) {
               Spacer(modifier = Modifier
                  .fillMaxWidth()
                  .height(15.dp))
            }
         }
      }

      Box(modifier = Modifier
         .align(Alignment.TopEnd)
         .padding(10.dp)
         .zIndex(1f)
      ) {
         Text(text = "${currentPage + 1}/$pageCount")
      }
   }
}

/**
 * PDF document viewer from [Uri]
 *
 * [modifier] - [Modifier] interface for the whole composable, by default fills max size available
 * [uri] - [Uri] from which document should be retrieved
 * [verticalArrangement] - Page arrangement
 */
@Composable
fun PdfViewer(
   modifier: Modifier = Modifier.fillMaxSize(),
   uri: Uri,
   verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp)
) {
   val rendererScope = rememberCoroutineScope()
   val mutex = remember { Mutex() }

   val context = LocalContext.current

   val renderer by produceState<PdfRenderer?>(null, uri) {
      rendererScope.launch(Dispatchers.IO) {
         val input = ParcelFileDescriptor.open(uri.toFile(), ParcelFileDescriptor.MODE_READ_ONLY)
         value = PdfRenderer(input)
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
   val imageLoader = context.imageLoader
   val imageLoadingScope = rememberCoroutineScope()
   BoxWithConstraints(modifier = modifier) {
      val width = with(LocalDensity.current) { maxWidth.toPx() }.toInt()
      val height = (width * sqrt(2f)).toInt()
      val pageCount by remember(renderer) { derivedStateOf { renderer?.pageCount ?: 0 } }

      val lazyListState = rememberLazyListState()
      var currentPage by remember { mutableStateOf(1) }

      // TODO: Оптимизировать отслеживание текущей страницы
      val fullyVisibleIndices: List<Int> by remember {
         derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
               emptyList()
            } else {
               val fullyVisibleItemsInfo = visibleItemsInfo.toMutableList()
               val lastItem = fullyVisibleItemsInfo.last()
               val viewportHeight = layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset
               if (lastItem.offset + lastItem.size > viewportHeight) { fullyVisibleItemsInfo.removeLast() }
               val firstItemIfLeft = fullyVisibleItemsInfo.firstOrNull()
               if (firstItemIfLeft != null && firstItemIfLeft.offset < layoutInfo.viewportStartOffset) {
                  fullyVisibleItemsInfo.removeFirst()
               }

               fullyVisibleItemsInfo.map { it.index }
            }
         }
      }

      LazyColumn(
         verticalArrangement = verticalArrangement,
         state = lazyListState
      ) {
         items(
            count = pageCount,
            key = { index -> "$uri-$index" }
         ) { index ->
            val isVisible by remember(index) {
               derivedStateOf { fullyVisibleIndices.contains(index) } }

            if (isVisible) { currentPage = index }

            val cacheKey = MemoryCache.Key("$uri-$index")
            val cacheValue : Bitmap? = imageLoader.memoryCache?.get(cacheKey)?.bitmap

            var bitmap : Bitmap? by remember { mutableStateOf(cacheValue)}
            if (bitmap == null) {
               DisposableEffect(uri, index) {
                  val job = imageLoadingScope.launch(Dispatchers.IO) {
                     val destinationBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                     mutex.withLock {
                        Log.d("PdfGenerator", "Loading PDF $uri - page ${index + 1}/$pageCount")
                        if (!coroutineContext.isActive) return@launch
                        try {
                           renderer?.let {
                              it.openPage(index).use { page ->
                                 page.render(destinationBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                              }
                           }
                        } catch (e: Exception) {
                           return@launch
                        }
                     }
                     bitmap = destinationBitmap
                  }
                  onDispose {
                     job.cancel()
                  }
               }
               Box(modifier = Modifier
                  .background(Color.White)
                  .aspectRatio(1f / sqrt(2f))
                  .fillMaxWidth())
            } else {
               val request = ImageRequest.Builder(context)
                  .size(width, height)
                  .memoryCacheKey(cacheKey)
                  .data(bitmap)
                  .build()

               Box {
                  var isEnlargedDialogDisplayed by remember { mutableStateOf(false) }
                  val interactionSource = MutableInteractionSource()

                  Icon(
                     modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(15.dp)
                        .zIndex(1f),
                     imageVector = Icons.Rounded.Search,
                     contentDescription = "Magnify Indicator"
                  )

                  Image(
                     modifier = Modifier
                        .background(Color.White)
                        .aspectRatio(1f / sqrt(2f))
                        .fillMaxWidth()
                        .clickable(
                           interactionSource = interactionSource,
                           indication = null
                        ) { isEnlargedDialogDisplayed = true },
                     contentScale = ContentScale.Fit,
                     painter = rememberAsyncImagePainter(request),
                     contentDescription = "Page ${index + 1} of $pageCount"
                  )

                  if (isEnlargedDialogDisplayed) {
                     ImageDialog(img = request) {
                        isEnlargedDialogDisplayed = false
                     }
                  }
               }
            }

            if (index != pageCount - 1) {
               Spacer(modifier = Modifier
                  .fillMaxWidth()
                  .height(15.dp))
            }
         }
      }

      Box(modifier = Modifier
         .align(Alignment.TopEnd)
         .padding(10.dp)
         .zIndex(1f)
      ) {
         Text(text = "${currentPage + 1}/$pageCount")
      }
   }
}

/**
 * PDF document viewer from URL
 *
 * [modifier] - [Modifier] interface for the whole composable, by default fills max size available
 * [url] - URL to PDF document
 * [documentDescription] - Similar to content description but cannot be null in this case
 * [verticalArrangement] - Page arrangement
 */
@Composable
fun PdfViewer(
   modifier: Modifier = Modifier.fillMaxSize(),
   url: String,
   documentDescription: String,
   verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp)
){

}


@Throws(IOException::class)
private fun copy(source: InputStream, target: OutputStream) {
   val buf = ByteArray(8192)
   var length: Int
   while (source.read(buf).also { length = it } > 0) {
      target.write(buf, 0, length)
   }
}
