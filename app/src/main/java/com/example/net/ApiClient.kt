package com.example.net

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ApiClient {
    const val BASE_URL = "https://fjalor.bashk.eu/api"

    val ok: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
