package com.example.todaydeparture.dto;

public class CreateRoomRequest {
    // Android가 보내는 필드명
    private String name;
    private String appointmentTime;
    // 서버 내부 필드명 (호환용)
    private String title;
    private String destination;
    private String meetingTime;

    // Jackson 역직렬화에 필요한 setter
    public void setName(String name) { this.name = name; }
    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }
    public void setTitle(String title) { this.title = title; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setMeetingTime(String meetingTime) { this.meetingTime = meetingTime; }

    // getter: Android 필드명 → 서버 필드명 폴백
    public String getTitle() {
        return (title != null && !title.isEmpty()) ? title : name;
    }
    public String getDestination() { return destination; }
    public String getMeetingTime() {
        return (meetingTime != null && !meetingTime.isEmpty()) ? meetingTime : appointmentTime;
    }
}