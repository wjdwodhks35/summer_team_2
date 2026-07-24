package com.example.todaydeparture.dto;

public class AuthResponse {
    private String accessToken;
    private Long userId;
    private String nickname;

    public AuthResponse(String accessToken, Long userId, String nickname) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.nickname = nickname;
    }

    public String getAccessToken() { return accessToken; }
    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
}