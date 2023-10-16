package ru.spektrit.lycoris.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

internal interface DownloadInterface {
   @Streaming
   @GET
   suspend fun downloadFile(@Url url: String): ResponseBody
}