package com.example.whentoleave.data.model

// ===== 로그인 =====
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val userId: Long,
    val nickname: String
)

// ===== 회원가입 =====
data class RegisterRequest(
    val email: String,
    val password: String,
    val nickname: String
)

data class RegisterResponse(
    val accessToken: String,
    val userId: Long,
    val nickname: String
)