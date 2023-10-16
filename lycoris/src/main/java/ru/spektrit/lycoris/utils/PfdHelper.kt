package ru.spektrit.lycoris.utils

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.annotation.RawRes
import androidx.core.net.toFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class PfdHelper {
   fun getPfd ( context : Context, @RawRes pdfResId : Int, documentDescription : String ) : ParcelFileDescriptor {
      val inputStream = context.resources.openRawResource(pdfResId)
      val outputDir = context.cacheDir
      val tempFile = File.createTempFile(documentDescription, "pdf", outputDir)
      tempFile.mkdirs()
      tempFile.deleteOnExit()
      val outputStream = FileOutputStream(tempFile)
      copy(inputStream, outputStream)

      return ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
   }

   fun getPfd( uri : Uri ) : ParcelFileDescriptor {
      return ParcelFileDescriptor.open(uri.toFile(), ParcelFileDescriptor.MODE_READ_ONLY)
   }


   @Throws(IOException::class)
   private fun copy(source: InputStream, target: OutputStream) {
      val buf = ByteArray(8192)
      var length: Int
      while (source.read(buf).also { length = it } > 0) { target.write(buf, 0, length) }
   }
}