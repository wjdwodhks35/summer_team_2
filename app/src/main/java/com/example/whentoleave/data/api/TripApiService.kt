package com.example.whentoleave.data.api

import com.example.whentoleave.data.model.TripRequest
import com.example.whentoleave.data.model.TripResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TripApiService {

    @POST("api/departure/recommend")
    suspend fun calculateTrip(@Body request: TripRequest): Response<TripResponse>
}