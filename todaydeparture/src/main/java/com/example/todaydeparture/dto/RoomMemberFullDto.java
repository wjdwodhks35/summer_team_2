package com.example.todaydeparture.dto;

public class RoomMemberFullDto {
    private Long userId;
    private String name;
    private boolean isHost;
    private boolean isLocationShared;
    private Double departureLat;
    private Double departureLon;
    private String departureAddress;
    private Integer estimatedTravelMin;
    private String recommendedDepartureTime;

    // MyBatis setters — alias "isHost" → setIsHost, "isLocationShared" → setIsLocationShared
    public void setUserId(Long userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setIsHost(boolean isHost) { this.isHost = isHost; }
    public void setIsLocationShared(boolean isLocationShared) { this.isLocationShared = isLocationShared; }
    public void setDepartureLat(Double departureLat) { this.departureLat = departureLat; }
    public void setDepartureLon(Double departureLon) { this.departureLon = departureLon; }
    public void setDepartureAddress(String departureAddress) { this.departureAddress = departureAddress; }
    public void setEstimatedTravelMin(Integer estimatedTravelMin) { this.estimatedTravelMin = estimatedTravelMin; }
    public void setRecommendedDepartureTime(String t) { this.recommendedDepartureTime = t; }

    // Jackson getters — get 접두사 사용해야 "isHost", "isLocationShared" 로 직렬화됨
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public boolean getIsHost() { return isHost; }
    public boolean getIsLocationShared() { return isLocationShared; }
    public Double getDepartureLat() { return departureLat; }
    public Double getDepartureLon() { return departureLon; }
    public String getDepartureAddress() { return departureAddress; }
    public Integer getEstimatedTravelMin() { return estimatedTravelMin; }
    public String getRecommendedDepartureTime() { return recommendedDepartureTime; }
}
