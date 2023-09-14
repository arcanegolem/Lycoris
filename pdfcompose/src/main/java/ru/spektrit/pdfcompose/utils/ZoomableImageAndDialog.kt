package ru.spektrit.pdfcompose.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage


@Composable
fun ZoomableImage(
   modifier: Modifier,
   minScale: Float = 1f,
   maxScale: Float = 3f,
   img : Any
) {
   ZoomableBox (
      modifier = modifier,
      minScale = minScale,
      maxScale = maxScale,
   ) {
      AsyncImage(
         modifier = Modifier
            .graphicsLayer(
               scaleX = scale,
               scaleY = scale,
               translationX = offsetX,
               translationY = offsetY
            ),
         model = img,
         contentDescription = null,
         alignment = Alignment.Center
      )
   }
}


@Composable
fun ZoomableBox(
   modifier: Modifier = Modifier,
   minScale: Float = 1f,
   maxScale: Float = 3f,
   content: @Composable ZoomableBoxScope.() -> Unit
) {
   var scale by remember { mutableStateOf(1f) }
   var offsetX by remember { mutableStateOf(0f) }
   var offsetY by remember { mutableStateOf(0f) }
   var size by remember { mutableStateOf(IntSize.Zero) }

   Box(
      modifier = modifier
         .clip(RectangleShape)
         .onSizeChanged { size = it }
         .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
               scale = maxOf(minScale, minOf(scale * zoom, maxScale))
               val maxX = (size.width * (scale - 1)) / 2
               val minX = -maxX
               offsetX = maxOf(minX, minOf(maxX, offsetX + pan.x))
               val maxY = (size.height * (scale - 1)) / 2
               val minY = -maxY
               offsetY = maxOf(minY, minOf(maxY, offsetY + pan.y))
            }
         },
      contentAlignment = Alignment.Center
   ) {
      val scope = ZoomableBoxScopeImplementation(scale, offsetX, offsetY)
      scope.content()
   }
}

interface ZoomableBoxScope {
   val scale: Float
   val offsetX: Float
   val offsetY: Float
}

private data class ZoomableBoxScopeImplementation(
   override val scale: Float,
   override val offsetX: Float,
   override val offsetY: Float
) : ZoomableBoxScope


@Composable
fun ImageDialog(
   img: Any,
   onDismissRequest : () -> Unit,
){
   Dialog(
      properties = DialogProperties(usePlatformDefaultWidth = false),
      onDismissRequest = onDismissRequest
   ) {
      Box(modifier = Modifier
         .fillMaxSize()
      ){
         ZoomableImage(
            modifier = Modifier
               .fillMaxSize()
               .background(color = Color.White),
            img = img
         )
         Button(
            modifier = Modifier
               .padding(bottom = 30.dp)
               .align(Alignment.BottomCenter),
            onClick = { onDismissRequest() }
         ) {
            Text("Назад")
         }
      }
   }
}