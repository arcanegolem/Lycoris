package ru.spektrit.lycoris.viewdocument

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlin.math.sqrt

@Composable
fun PagePlaceHolder(backgroundColor : Color) {
   Box(
      modifier = Modifier
         .background(backgroundColor)
         .aspectRatio(1f / sqrt(2f))
         .fillMaxWidth()
   )
}