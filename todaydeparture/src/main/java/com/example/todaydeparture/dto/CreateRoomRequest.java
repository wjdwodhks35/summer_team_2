package com.example.todaydeparture.dto;

public class CreateRoomRequest {
    private String name;
    private String appointmentTime;
    private String title;
    private String destination;
    private String meetingTime;
    private Double destinationLatitude;
    private Double destinationLongitude;
    // Android 앱이 보내는 단축 필드명도 수용
    private Double destLat;
    private Double destLon;
    private String purposeCode;

    public void setName(String name) { this.name = name; }
    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }
    public void setTitle(String title) { this.title = title; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setMeetingTime(String meetingTime) { this.meetingTime = meetingTime; }
    public void setDestinationLatitude(Double destinationLatitude) { this.destinationLatitude = destinationLatitude; }
    public void setDestinationLongitude(Double destinationLongitude) { this.destinationLongitude = destinationLongitude; }
    public void setDestLat(Double destLat) { this.destLat = destLat; }
    public void setDestLon(Double destLon) { this.destLon = destLon; }
    public void setPurposeCode(String purposeCode) { this.purposeCode = purposeCode; }

    public String getTitle() {
        return (title != null && !title.isBlank()) ? title : name;
    }

    public String getDestination() { return destination; }

    public String getMeetingTime() {
        return (meetingTime != null && !meetingTime.isBlank()) ? meetingTime : appointmentTime;
    }

    public Double getDestinationLatitude() {
        return (destinationLatitude != null) ? destinationLatitude : destLat;
    }
    public Double getDestinationLongitude() {
        return (destinationLongitude != null) ? destinationLongitude : destLon;
    }

    public String getPurposeCode() {
        return (purposeCode != null && !purposeCode.isBlank()) ? purposeCode : "general";
    }
}
