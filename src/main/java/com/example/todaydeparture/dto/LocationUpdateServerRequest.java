package com.example.todaydeparture.dto;

public class LocationUpdateServerRequest {
    private Double lat;
    private Double lon;
    private String address;
    private Boolean isShared;

    public void setLat(Double lat) { this.lat = lat; }
    public void setLon(Double lon) { this.lon = lon; }
    public void setAddress(String address) { this.address = address; }
    public void setIsShared(Boolean isShared) { this.isShared = isShared; }
    public void setShared(Boolean isShared) { this.isShared = isShared; }

    public Double getLat() { return lat; }
    public Double getLon() { return lon; }
    public String getAddress() { return address; }
    public Boolean getIsShared() { return isShared; }
}
