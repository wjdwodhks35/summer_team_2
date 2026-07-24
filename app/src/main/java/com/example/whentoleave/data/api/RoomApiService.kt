package com.example.whentoleave.data.api

import com.example.whentoleave.data.model.*
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface RoomApiService {

    // 방 생성
    @POST("api/rooms")
    suspend fun createRoom(
        @Header("Authorization") token: String,
        @Body request: CreateRoomRequest
    ): Response<CreateRoomResponse>

    // 방 참여
    @POST("api/rooms/join")
    suspend fun joinRoom(
        @Header("Authorization") token: String,
        @Body request: JoinRoomRequest
    ): Response<JoinRoomResponse>

    // 방 정보 조회
    @GET("api/rooms/{id}")
    suspend fun getRoomInfo(
        @Header("Authorization") token: String,
        @Path("id") roomId: Long
    ): Response<RoomInfoResponse>

    // 내 위치 업로드
    @PUT("api/rooms/{id}/location")
    suspend fun updateLocation(
        @Header("Authorization") token: String,
        @Path("id") roomId: Long,
        @Body request: LocationUpdateRequest
    ): Response<Unit>

    companion object {
        // 에뮬레이터: 10.0.2.2 = 호스트 PC / 실제 기기: 핫스팟 IP
        private fun baseUrl(): String {
            val fp = android.os.Build.FINGERPRINT
            val isEmu = fp.startsWith("generic") || fp.startsWith("unknown") ||
                android.os.Build.MODEL.contains("Emulator", ignoreCase = true) ||
                android.os.Build.MODEL.contains("Android SDK", ignoreCase = true) ||
                android.os.Build.BRAND.startsWith("generic") ||
                android.os.Build.DEVICE.startsWith("generic")
            return if (isEmu) "http://10.0.2.2:8080/" else "http://172.20.10.7:8080/"
        }

        fun create(): RoomApiService = Retrofit.Builder()
            .baseUrl(baseUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RoomApiService::class.java)
    }
}