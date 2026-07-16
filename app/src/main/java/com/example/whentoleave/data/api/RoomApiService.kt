package com.example.whentoleave.data.api

import com.example.whentoleave.data.model.CreateRoomRequest
import com.example.whentoleave.data.model.JoinRoomRequest
import com.example.whentoleave.data.model.RoomMember
import com.example.whentoleave.data.model.RoomResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RoomApiService {

    @POST("api/rooms")
    suspend fun createRoom(@Body request: CreateRoomRequest): Response<RoomResponse>

    @POST("api/rooms/join")
    suspend fun joinRoom(@Body request: JoinRoomRequest): Response<RoomResponse>

    @GET("api/rooms/{roomId}/members")
    suspend fun getRoomMembers(@Path("roomId") roomId: Long): Response<List<RoomMember>>
}