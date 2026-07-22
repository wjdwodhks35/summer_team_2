package com.example.whentoleave.data.model

// ===== 길찾기 요청 =====
data class TripRequest(
    val startAddress: String,
    val destinationAddress: String,    // endAddress → destinationAddress
    val targetDateTime: String,         // "2026-07-14T19:00:00"
    val purposeCode: String,            // purpose → purposeCode
    val preferredTransport: String = "SUBWAY",
    val baseTravelMinutes: Int = 30   // 추가 — 기본 30분
)

// ===== 길찾기 결과 =====
data class TripResponse(
    val tripRequestId: Long?,
    val tripResultId: Long?,
    val recommendedDepartureTime: String,
    val expectedArrivalTime: String,    // estimatedArrivalTime → expectedArrivalTime
    val baseTravelMinutes: Int,
    val purposeBufferMinutes: Int,
    val personalBufferMinutes: Int,
    val totalBufferMinutes: Int,
    val riskLevel: String?,
    val summary: String?                // recommendationNote → summary
)