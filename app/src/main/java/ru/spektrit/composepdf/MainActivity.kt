package ru.spektrit.composepdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import ru.spektrit.composepdf.ui.theme.ComposePDFTheme
import ru.spektrit.pdfcompose.viewdocument.PdfViewer

class MainActivity : ComponentActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
         ComposePDFTheme {
            Surface(
               modifier = Modifier.fillMaxSize(),
               color = MaterialTheme.colorScheme.background
            ) {
               PdfViewer(pdfResId = R.raw.gost_23858_79, documentDescription = "pdfviewer_libtest")
            }
         }
      }
   }
}
