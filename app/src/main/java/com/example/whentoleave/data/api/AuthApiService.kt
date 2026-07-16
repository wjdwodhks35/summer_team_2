package com.example.whentoleave.data.api

import com.example.whentoleave.data.model.LoginRequest
import com.example.whentoleave.data.model.LoginResponse
import com.example.whentoleave.data.model.RegisterRequest
import com.example.whentoleave.data.model.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>
}