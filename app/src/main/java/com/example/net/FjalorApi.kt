package com.example.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder

class FjalorApi(private val client: OkHttpClient = ApiClient.ok) {

    suspend fun search(q: String): Result<List<Entry>> = withContext(Dispatchers.IO) {
        val trimmed = q.trim()
        if (trimmed.isEmpty()) return@withContext Result.Ok(emptyList())

        val url = HttpUrl.Builder()
            .scheme("https")
            .host("fjalor.bashk.eu")
            .addPathSegment("api")
            .addPathSegment("search")
            .addQueryParameter("q", trimmed)
            .build()

        executeGetList(url.toString())
    }

    suspend fun word(slug: String): Result<List<Entry>> = withContext(Dispatchers.IO) {
        val trimmed = slug.trim().lowercase()
        if (trimmed.isEmpty()) return@withContext Result.Empty

        val encodedSlug = URLEncoder.encode(trimmed, "UTF-8").replace("+", "%20")
        val url = "${ApiClient.BASE_URL}/word/$encodedSlug"

        executeGetList(url)
    }

    suspend fun random(n: Int = 20): Result<List<Entry>> = withContext(Dispatchers.IO) {
        val clampedN = n.coerceIn(1, 50)
        val url = "${ApiClient.BASE_URL}/random?n=$clampedN"
        executeGetList(url)
    }

    suspend fun slugs(): Result<List<String>> = withContext(Dispatchers.IO) {
        val url = "${ApiClient.BASE_URL}/slugs"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext if (response.code == 404) Result.Empty else Result.Http(response.code)
                }
                val body = response.body?.string() ?: return@withContext Result.Empty
                val list = JsonParsers.parseStringList(body)
                if (list.isEmpty()) Result.Empty else Result.Ok(list)
            }
        } catch (e: IOException) {
            Result.Offline
        } catch (e: Exception) {
            Result.Err(e.message ?: "Gabim gjatë shkarkimit të fjalëve")
        }
    }

    private fun executeGetList(url: String): Result<List<Entry>> {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        Result.Empty
                    } else {
                        Result.Http(response.code)
                    }
                } else {
                    val body = response.body?.string() ?: return Result.Empty
                    val list = JsonParsers.parseEntryList(body)
                    if (list.isEmpty()) {
                        Result.Empty
                    } else {
                        Result.Ok(list)
                    }
                }
            }
        } catch (e: IOException) {
            Result.Offline
        } catch (e: Exception) {
            Result.Err(e.message ?: "Gabim gjatë ngarkimit")
        }
    }
}
