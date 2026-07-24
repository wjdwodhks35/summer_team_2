package com.example.whentoleave.data.model

// ── 방 생성 ──────────────────────────────────────────────
data class CreateRoomRequest(
    val name: String,
    val destination: String,     // 최종 목적지 주소
    val destLat: Double,
    val destLon: Double,
    val appointmentTime: String  // "yyyy-MM-dd HH:mm"
)

data class CreateRoomResponse(
    val id: Long,
    val name: String,
    val code: String,            // 초대 코드 (예: 72K9-XQ)
    val destination: String,
    val destLat: Double,
    val destLon: Double,
    val appointmentTime: String,
    val hostName: String,
    val createdAt: String
)

// ── 방 참여 ──────────────────────────────────────────────
data class JoinRoomRequest(
    val code: String
)

data class JoinRoomResponse(
    val id: Long,
    val name: String,
    val code: String,
    val destination: String,
    val appointmentTime: String,
    val hostName: String
)

// ── 방 정보 조회 ─────────────────────────────────────────
data class RoomInfoResponse(
    val id: Long,
    val name: String,
    val code: String,
    val destination: String,
    val destLat: Double,
    val destLon: Double,
    val appointmentTime: String,
    val hostName: String,
    val memberCount: Int,
    val members: List<RoomMember>
)

data class RoomMember(
    val userId: Long,
    val name: String,
    val isHost: Boolean,
    val isLocationShared: Boolean,
    val departureLat: Double?,
    val departureLon: Double?,
    val departureAddress: String?,       // 출발지 주소 (예: 고려대학교 정문 부근)
    val estimatedTravelMin: Int?,        // 예상 소요 시간(분) — 서버 or 앱에서 계산
    val recommendedDepartureTime: String? // "HH:mm" — 약속시간 - 소요시간
)

// ── 출발지 업로드 ─────────────────────────────────────────
data class LocationUpdateRequest(
    val lat: Double,
    val lon: Double,
    val address: String,
    val isShared: Boolean = true
)

// ── 위치 비공개 ───────────────────────────────────────────
data class LocationHideRequest(
    val isShared: Boolean = false
)