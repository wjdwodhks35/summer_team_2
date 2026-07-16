package com.example.whentoleave.data.model

// ===== 방 생성 =====
data class CreateRoomRequest(
    val title: String,
    val destination: String,
    val meetingTime: String   // "2026-07-14T19:00:00"
)

data class RoomResponse(
    val roomId: Long,
    val title: String,
    val destination: String,
    val meetingTime: String,
    val inviteCode: String,
    val memberCount: Int
)

// ===== 방 참여 =====
data class JoinRoomRequest(
    val inviteCode: String,
    val guestToken: String?   // 비회원이면 UUID 전달
)

// ===== 방 멤버 =====
data class RoomMember(
    val memberId: Long,
    val nickname: String,
    val currentLocation: String?,
    val recommendedDepartureTime: String?,
    val isReady: Boolean
)