package com.example.alertapp.android.api

import com.example.alertapp.android.processors.ContentData
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Url

interface ContentApi {
    @GET
    @Headers("User-Agent: AlertApp/1.0")
    suspend fun getContent(
        @Url url: String,
        @Header("If-Modified-Since") ifModifiedSince: String? = null
    ): Response<ResponseBody>
}

class ContentApiImpl(
    private val api: ContentApi
) : ContentApi {
    override suspend fun getContent(
        url: String,
        ifModifiedSince: String?
    ): Response<ResponseBody> {
        return api.getContent(url, ifModifiedSince)
    }

    fun mapToContentData(url: String, response: Response<ResponseBody>): ContentData {
        val headers = response.headers().toMultimap().mapValues { it.value.firstOrNull() ?: "" }
        val lastModified = headers["Last-Modified"]?.let {
            parseHttpDate(it)
        } ?: System.currentTimeMillis()

        return ContentData(
            url = url,
            text = response.body()?.string() ?: "",
            statusCode = response.code(),
            lastModified = lastModified,
            contentType = headers["Content-Type"] ?: "",
            headers = headers.filterValues { it.isNotEmpty() }
        )
    }

    private fun parseHttpDate(dateStr: String): Long {
        return try {
            val format = java.text.SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss zzz",
                java.util.Locale.US
            )
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
