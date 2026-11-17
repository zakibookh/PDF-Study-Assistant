package com.example.data.remote

import com.example.BuildConfig
import com.example.data.local.SessionPreferences
import com.example.data.model.ApiErrorResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClientFactory {
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Volatile
    var authToken: String? = null

    @Volatile
    private var currentBaseUrl: String = SessionPreferences.normalizeBaseUrl(BuildConfig.API_BASE_URL)

    @Volatile
    private var cachedService: StudyApiService? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = authToken
        val request = if (!token.isNullOrBlank() && original.header("Authorization") == null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        chain.proceed(request)
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            },
        )
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @Synchronized
    fun getService(baseUrl: String? = null): StudyApiService {
        val targetUrl = SessionPreferences.normalizeBaseUrl(baseUrl ?: currentBaseUrl)
        val existing = cachedService
        if (existing != null && targetUrl == currentBaseUrl) {
            return existing
        }
        currentBaseUrl = targetUrl
        val retrofit = Retrofit.Builder()
            .baseUrl(targetUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        return retrofit.create(StudyApiService::class.java).also {
            cachedService = it
        }
    }

    fun getCurrentBaseUrl(): String = currentBaseUrl

    fun parseError(throwable: Throwable): String {
        return when (throwable) {
            is HttpException -> {
                val code = throwable.code()
                val rawBody = runCatching { throwable.response()?.errorBody()?.string() }.getOrNull()
                val parsedMessage = if (!rawBody.isNullOrBlank()) {
                    runCatching {
                        moshi.adapter(ApiErrorResponse::class.java).fromJson(rawBody)?.error
                    }.getOrNull()
                } else {
                    null
                }
                when {
                    !parsedMessage.isNullOrBlank() -> parsedMessage
                    code == 401 -> "Session expired or invalid credentials (401)."
                    code == 403 -> "Permission denied — administrator role required (403)."
                    code == 404 -> "Endpoint not found (404). Make sure the v1 backend worker is deployed at your Settings URL."
                    code == 429 -> "Too many requests (429). Please wait a moment."
                    else -> "Server error ($code): ${throwable.message()}"
                }
            }
            is IOException -> "Network error: unable to reach ${currentBaseUrl}. Check your internet or Server URL in Settings."
            else -> throwable.localizedMessage ?: "Unexpected error occurred."
        }
    }
}
