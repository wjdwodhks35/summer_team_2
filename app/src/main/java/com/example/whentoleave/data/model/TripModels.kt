package com.example.whentoleave.data.model

// ===== 길찾기 요청 =====
data class TripRequest(
    val startAddress: String,
    val endAddress: String,
    val targetTime: String,    // "2026-07-14T19:00:00"
    val timeType: String,      // "ARRIVAL" or "DEPARTURE"
    val purpose: String        // "GENERAL", "SCHOOL", "WORK", "APPOINTMENT", "EXAM", "INTERVIEW", "HOSPITAL"
)

// ===== 길찾기 결과 =====
data class TripResponse(
    val tripResultId: Long,
    val recommendedDepartureTime: String,  // "18:14"
    val estimatedArrivalTime: String,      // "19:00"
    val totalTravelMinutes: Int,
    val transportType: String,             // "SUBWAY", "BUS", "WALK"
    val seatProbability: Int,              // 0~100
    val hasRain: Boolean,
    val rainTime: String?,
    val routeSummary: String,
    val timeBreakdown: TimeBreakdown,
    val recommendationNote: String
)

data class TimeBreakdown(
    val baseTravelMinutes: Int,
    val waitMinutes: Int,
    val congestionMinutes: Int,
    val weatherMinutes: Int,
    val trafficMinutes: Int,
    val purposeMinutes: Int,
    val personalMinutes: Int
)