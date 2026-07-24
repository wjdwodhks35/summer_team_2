package com.example.whentoleave.data.api

import android.content.Context
import com.example.whentoleave.util.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 에뮬레이터에서는 10.0.2.2 = 호스트 PC. 실제 기기에서는 핫스팟 IP.
    private val BASE_URL: String get() = if (isEmulator()) "http://10.0.2.2:8080/" else "http://172.20.10.7:8080/"

    private fun isEmulator(): Boolean {
        val fp = android.os.Build.FINGERPRINT
        return fp.startsWith("generic") || fp.startsWith("unknown") ||
               android.os.Build.MODEL.contains("google_sdk", ignoreCase = true) ||
               android.os.Build.MODEL.contains("Emulator", ignoreCase = true) ||
               android.os.Build.MODEL.contains("Android SDK", ignoreCase = true) ||
               android.os.Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
               android.os.Build.BRAND.startsWith("generic") ||
               android.os.Build.DEVICE.startsWith("generic")
    }

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val token = appContext?.let { TokenManager.getToken(it) }
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else chain.request()
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // lazy 제거: BASE_URL이 런타임에 결정되므로 매 접근 시 생성
    val instance: Retrofit
        get() = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}