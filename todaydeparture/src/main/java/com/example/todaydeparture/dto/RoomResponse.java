package com.example.todaydeparture.dto;

public class RoomResponse {
    private Long roomId;
    private String title;
    private String destination;
    private String meetingTime;
    private String inviteCode;
    private int memberCount;

    public RoomResponse(Long roomId, String title, String destination,
                        String meetingTime, String inviteCode, int memberCount) {
        this.roomId = roomId;
        this.title = title;
        this.destination = destination;
        this.meetingTime = meetingTime;
        this.inviteCode = inviteCode;
        this.memberCount = memberCount;
    }

    public Long getRoomId() { return roomId; }
    public String getTitle() { return title; }
    public String getDestination() { return destination; }
    public String getMeetingTime() { return meetingTime; }
    public String getInviteCode() { return inviteCode; }
    public int getMemberCount() { return memberCount; }
}