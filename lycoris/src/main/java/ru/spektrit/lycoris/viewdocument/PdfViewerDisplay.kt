package ru.spektrit.lycoris.viewdocument

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
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

@Composable
internal fun PdfViewerDisplay(
   modifier : Modifier = Modifier,
   context : Context,
   renderer: PdfRenderer?,
   documentIdentifier : String,
   pagesVerticalArrangement: Arrangement.Vertical,
   mutex : Mutex,
   accentColor : Color,
   iconTint : Color,
   controlsAlignment: Alignment,
   bitmapScale : Int
) {
   val pdfViewerDisplayScope = rememberCoroutineScope()
   val imageLoadingScope = rememberCoroutineScope()

   val imageLoader = context.imageLoader

   var viewerScale by remember { mutableFloatStateOf(1f) }
   var viewerOffset by remember { mutableStateOf(Offset.Zero) }

   BoxWithConstraints(modifier = modifier) {
      val pageCount by remember(renderer) { derivedStateOf { renderer?.pageCount ?: 0 } }

      val lazyListState = rememberLazyListState()
      var currentPage by remember { mutableIntStateOf(1) }

      // TODO: Optimize current page tracking
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

      // Page count for some reason always starts with 2, so check is precise
      if (pageCount > 2) {
         Column(
            modifier = Modifier
               .align(controlsAlignment)
               .padding(8.dp)
               .zIndex(2f)
         ) {
            IconButton(
               onClick = { viewerScale = (viewerScale + 0.5f).coerceIn(1f, 10f) },
               colors = IconButtonDefaults.iconButtonColors(
                  containerColor = accentColor.copy(alpha = 0.4f)
               )
            ) {
               Icon(
                  imageVector = Icons.Rounded.ZoomIn,
                  contentDescription = "Zoom in",
                  tint = iconTint
               )
            }
            IconButton(
               onClick = {
                  viewerScale = 1f
                  viewerOffset = Offset(0f, 0f)
               },
               colors = IconButtonDefaults.iconButtonColors(
                  containerColor = accentColor.copy(alpha = 0.4f)
               )
            ) {
               Icon(
                  imageVector = Icons.Rounded.ZoomOut,
                  contentDescription = "Zoom out",
                  tint = iconTint
               )
            }
         }
      }


      val transformableState = rememberTransformableState { _, panChange, _ ->
         val extraWidth = (viewerScale - 1) * constraints.maxWidth
         val extraHeight = (viewerScale - 1) * constraints.maxHeight

         val maxX = extraWidth / 2
         val maxY = extraHeight / 2

         viewerOffset = Offset(
            x = (viewerOffset.x + panChange.x * viewerScale).coerceIn(-maxX, maxX),
            y = (viewerOffset.y + (panChange.y / 2) * viewerScale).coerceIn(-maxY, maxY),
         )
         pdfViewerDisplayScope.launch{ lazyListState.scrollBy(-(panChange.y / 2)) }
      }
      if (pageCount > 2) {
         LazyColumn(
            userScrollEnabled = viewerScale == 1f,
            modifier = Modifier
               .graphicsLayer {
                  scaleX = viewerScale
                  scaleY = viewerScale
                  translationX = viewerOffset.x
                  translationY = viewerOffset.y
               }
               .transformable(transformableState),
            verticalArrangement = pagesVerticalArrangement,
            state = lazyListState
         ) {
            items(
               count = pageCount,
               key = { index -> "$documentIdentifier-$index" }
            ) { index ->
               val isVisible by remember(index) {
                  derivedStateOf { fullyVisibleIndices.contains(index) }
               }

               if (isVisible) {
                  currentPage = index
               }

               val cacheKey = MemoryCache.Key("$documentIdentifier-$index")
               val cachedBitmap: Bitmap? = imageLoader.memoryCache?.get(cacheKey)?.bitmap
               var bitmap: Bitmap? by remember { mutableStateOf(cachedBitmap) }

               if (bitmap == null) {

                  DisposableEffect(documentIdentifier, index) {
                     var destinationBitmap: Bitmap? = null
                     val job = imageLoadingScope.launch(Dispatchers.IO) {
                        mutex.withLock {
                           Log.d(
                              "PdfGenerator",
                              "Loading PDF $documentIdentifier - page ${index + 1}/$pageCount"
                           )
                           if (!coroutineContext.isActive) return@launch
                           try {
                              renderer?.let {
                                 it.openPage(index).use { page ->
                                    destinationBitmap = Bitmap.createBitmap(
                                       page.width * bitmapScale,
                                       page.height * bitmapScale,
                                       Bitmap.Config.ARGB_8888
                                    )
                                    page.render(
                                       destinationBitmap!!,
                                       null,
                                       null,
                                       PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                    )
                                 }
                              }
                           } catch (e: Exception) {
                              return@launch
                           }
                        }
                        bitmap = destinationBitmap
                     }
                     onDispose { job.cancel() }
                  }

                  PagePlaceHolder(Color.White)
               } else {
                  val density = LocalDensity.current.density.toInt()
                  val pageWidth = bitmap!!.getScaledWidth(density)
                  val pageHeight = bitmap!!.getScaledHeight(density)
                  Log.i(
                     "Dimensions",
                     "$pageWidth : $pageHeight, aspect ratio = ${pageWidth / pageHeight}"
                  )

                  val request = ImageRequest.Builder(context)
                     .size(pageWidth, pageHeight)
                     .memoryCacheKey(cacheKey)
                     .data(bitmap)
                     .build()

                  Box {
                     Image(
                        modifier = Modifier
                           .background(Color.White)
                           .fillMaxWidth()
                           .aspectRatio((pageWidth.toFloat() / pageHeight.toFloat())),
                        contentScale = ContentScale.Fit,
                        painter = rememberAsyncImagePainter(request),
                        contentDescription = "Page ${index + 1} of $pageCount"
                     )
                  }
               }

               if (index != pageCount - 1) {
                  Spacer(
                     modifier = Modifier
                        .fillMaxWidth()
                        .height(15.dp)
                  )
               }
            }
         }
      } else {
         CircularProgressIndicator(
            modifier = Modifier
               .fillMaxWidth(0.3f)
               .align(Alignment.Center),
            color = accentColor,
            strokeCap = StrokeCap.Round
         )
      }

      // Page count for some reason always starts with 2, so check is precise
      if (pageCount > 2) {
         Text(
            modifier = Modifier
               .align(Alignment.TopEnd)
               .padding(10.dp)
               .zIndex(1f),
            text = "${currentPage + 1}/$pageCount"
         )
      }
   }
}