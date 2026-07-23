package com.example.todaydeparture.dto;

public class CreateRoomRequest {
    private String title;
    private String destination;
    private String meetingTime;

    public String getTitle() { return title; }
    public String getDestination() { return destination; }
    public String getMeetingTime() { return meetingTime; }
}