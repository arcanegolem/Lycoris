package ru.spektrit.lycoris.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit

internal fun provideRetrofit(
   headers: HashMap<String, String>
): Retrofit = Retrofit.Builder().baseUrl("https://www.google.com").client(provideClient(headers)).build()

internal fun provideClient(
   headers: HashMap<String, String>
): OkHttpClient = OkHttpClient.Builder().addInterceptor(DownloadInterceptor(headers)).build()


internal fun provideDownloadInterface(
   headers: HashMap<String,String>
): DownloadInterface = provideRetrofit(headers).create(DownloadInterface::class.java)
