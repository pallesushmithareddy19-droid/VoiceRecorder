package com.twinmind.voicenotes.net

import com.twinmind.voicenotes.BuildConfig
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface AzureApi {
    @Multipart
    @POST("openai/deployments/{deployment}/audio/transcriptions")
    suspend fun transcribe(
        @Path("deployment") deployment: String = BuildConfig.AZURE_DEPLOYMENT_TRANSCRIBE,
        @Query("api-version") apiVersion: String = BuildConfig.AZURE_API_VERSION,
        @Part file: MultipartBody.Part
    ): ResponseBody
}
