package com.twinmind.voicenotes.net

import com.twinmind.voicenotes.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File

object Net {
    val ioScope = CoroutineScope(Dispatchers.IO)

    private val client by lazy {
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder()
            .addInterceptor(log)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("api-key", BuildConfig.AZURE_OPENAI_API_KEY)
                    .build()
                chain.proceed(req)
            }
            .build()
    }

    private val retrofit by lazy {
        val base = BuildConfig.AZURE_OPENAI_ENDPOINT.let {
            if (it.endsWith("/")) it else "$it/"
        }
        Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    private val api by lazy { retrofit.create(AzureApi::class.java) }

    suspend fun transcribeWav(path: String): String {
        val file = File(path)
        val part = MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody("audio/wav".toMediaType())
        )
        val body = api.transcribe(file = part)
        val s = body.string()
        val text = Regex("\"text\"\s*:\s*\"(.*?)\"", RegexOption.DOT_MATCHES_ALL)
            .find(s)?.groupValues?.getOrNull(1)
        return text ?: s
    }
}
