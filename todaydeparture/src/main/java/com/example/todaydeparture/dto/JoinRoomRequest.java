package com.example.todaydeparture.dto;

public class JoinRoomRequest {
    private String inviteCode;
    private String guestToken;

    public String getInviteCode() { return inviteCode; }
    public String getGuestToken() { return guestToken; }
}