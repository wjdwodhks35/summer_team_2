package com.example.todaydeparture.dto;

public class JoinRoomRequest {
    private String inviteCode;
    private String code;        // Android가 보내는 필드명
    private String guestToken;
    private String nickname;

    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public void setCode(String code) { this.code = code; }
    public void setGuestToken(String guestToken) { this.guestToken = guestToken; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getInviteCode() {
        return (inviteCode != null && !inviteCode.isEmpty()) ? inviteCode : code;
    }
    public String getGuestToken() { return guestToken; }
    public String getNickname() { return nickname; }
}
