package com.example.todaydeparture.dto;

public class RoomMemberDto {
    private Long memberId;
    private String nickname;
    private String currentLocation;
    private String recommendedDepartureTime;
    private boolean isReady;

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }
    public String getRecommendedDepartureTime() { return recommendedDepartureTime; }
    public void setRecommendedDepartureTime(String t) { this.recommendedDepartureTime = t; }
    public boolean isReady() { return isReady; }
    public void setReady(boolean ready) { isReady = ready; }
}