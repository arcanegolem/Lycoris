package ru.spektrit.composepdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.spektrit.composepdf.ui.theme.ComposePDFTheme
import ru.spektrit.lycoris.viewdocument.PdfViewer

class MainActivity : ComponentActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
         ComposePDFTheme {
            Column(
               modifier = Modifier.fillMaxSize(),
            ) {
               PdfViewer(
                  pdfResId = R.raw.sample_multipage,
                  controlsAlignment = Alignment.CenterEnd,
               )
            }
         }
      }
   }
}
