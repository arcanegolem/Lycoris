package ru.spektrit.lycoris.viewdocument

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
import androidx.compose.material3.LinearProgressIndicator
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
import ru.spektrit.lycoris.utils.ImageDialog
import ru.spektrit.lycoris.utils.PfdHelper
import ru.spektrit.lycoris.utils.provideFileName
import java.io.File
import kotlin.math.sqrt

/**
 * PDF document viewer (via [LazyColumn]) from [RawRes]"
 *
 * @param [modifier] [Modifier] interface for the whole composable, by default fills max size available
 * @param [pdfResId] Raw resource id of a PDF document
 * @param [documentDescription] Similar to content description but cannot be null in this case
 * @param [verticalArrangement] Page arrangement
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
         val pfdHelper = PfdHelper()
         value = PdfRenderer(pfdHelper.getPfd(context, pdfResId, documentDescription))
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
                        } catch (e: Exception) { return@launch }
                     }
                     bitmap = destinationBitmap
                  }
                  onDispose { job.cancel() }
               }
               PagePlaceHolder(Color.White)
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
                     ImageDialog(img = request) { isEnlargedDialogDisplayed = false }
                  }
               }
            }

            if (index != pageCount - 1) { Spacer(modifier = Modifier.fillMaxWidth().height(15.dp)) }
         }
      }

      Text(
         modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).zIndex(1f),
         text = "${currentPage + 1}/$pageCount"
      )
   }
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
   modifier: Modifier = Modifier.fillMaxSize(),
   uri: Uri,
   verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp)
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
                        } catch (e: Exception) { return@launch }
                     }
                     bitmap = destinationBitmap
                  }
                  onDispose { job.cancel() }
               }
               PagePlaceHolder(Color.White)
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
                     ImageDialog(img = request) { isEnlargedDialogDisplayed = false }
                  }
               }
            }

            if (index != pageCount - 1) { Spacer(modifier = Modifier.fillMaxWidth().height(15.dp)) }
         }
      }

      Text(
         modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).zIndex(1f),
         text = "${currentPage + 1}/$pageCount"
      )
   }
}

/**
 * PDF document viewer (via [LazyColumn]) from [Url]
 *
 * @param [modifier] [Modifier] interface for the whole composable, by default fills max size available
 * @param [url] [Url] where PDF document is stored at
 * @param [verticalArrangement] Page arrangement
 */
@Composable
fun PdfViewer(
   modifier: Modifier = Modifier.fillMaxSize(),
   @Url url: String,
   headers: HashMap<String, String>,
   verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp)
) {

   val rendererScope = rememberCoroutineScope()
   val mutex = remember { Mutex() }

   var docLoadPercentage by remember { mutableStateOf( 0 ) }
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

      if (docLoadPercentage < 100) {
         LinearProgressIndicator(
            modifier = Modifier
               .fillMaxWidth()
               .zIndex(1f)
               .align(Alignment.TopCenter),
            progress = docLoadPercentage / 100f,
            color = Color.Cyan,
            trackColor = Color.DarkGray
         )
      }

      LazyColumn(
         verticalArrangement = verticalArrangement,
         state = lazyListState
      ) {
         items(
            count = pageCount,
            key = { index -> "$url-$index" }
         ) { index ->
            val isVisible by remember(index) { derivedStateOf { fullyVisibleIndices.contains(index) } }

            if (isVisible) { currentPage = index }

            val cacheKey = MemoryCache.Key("$url-$index")
            val cacheValue : Bitmap? = imageLoader.memoryCache?.get(cacheKey)?.bitmap

            var bitmap : Bitmap? by remember { mutableStateOf(cacheValue)}
            if (bitmap == null) {
               DisposableEffect(url, index) {
                  val job = imageLoadingScope.launch(Dispatchers.IO) {
                     val destinationBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                     mutex.withLock {
                        Log.d("PdfGenerator", "Loading PDF $url - page ${index + 1}/$pageCount")
                        if (!coroutineContext.isActive) return@launch
                        try {
                           renderer?.let {
                              it.openPage(index).use { page ->
                                 page.render(destinationBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                              }
                           }
                        } catch (e: Exception) { return@launch }
                     }
                     bitmap = destinationBitmap
                  }
                  onDispose { job.cancel() }
               }
               PagePlaceHolder(Color.White)
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
                     ImageDialog(img = request) { isEnlargedDialogDisplayed = false }
                  }
               }
            }

            if (index != pageCount - 1) { Spacer(modifier = Modifier.fillMaxWidth().height(15.dp)) }
         }
      }

      if (pageCount > 0) {
         Text(
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).zIndex(1f),
            text = "${currentPage + 1}/$pageCount"
         )
      }
   }
}



