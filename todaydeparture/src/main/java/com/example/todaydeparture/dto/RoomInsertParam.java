package com.example.todaydeparture.dto;

public class RoomInsertParam {
    private Long id;
    private Long hostUserId;
    private String title;
    private String destinationAddress;
    private Double destinationLatitude;
    private Double destinationLongitude;
    private String meetingDatetime;
    private String purposeCode;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getHostUserId() { return hostUserId; }
    public void setHostUserId(Long hostUserId) { this.hostUserId = hostUserId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDestinationAddress() { return destinationAddress; }
    public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }
    public Double getDestinationLatitude() { return destinationLatitude; }
    public void setDestinationLatitude(Double destinationLatitude) { this.destinationLatitude = destinationLatitude; }
    public Double getDestinationLongitude() { return destinationLongitude; }
    public void setDestinationLongitude(Double destinationLongitude) { this.destinationLongitude = destinationLongitude; }
    public String getMeetingDatetime() { return meetingDatetime; }
    public void setMeetingDatetime(String meetingDatetime) { this.meetingDatetime = meetingDatetime; }
    public String getPurposeCode() { return purposeCode; }
    public void setPurposeCode(String purposeCode) { this.purposeCode = purposeCode; }
}
