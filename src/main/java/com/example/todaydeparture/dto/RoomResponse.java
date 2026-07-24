package com.example.todaydeparture.dto;

public class RoomResponse {
    private Long roomId;
    private String title;
    private String destination;
    private String meetingTime;
    private String inviteCode;
    private int memberCount;
    private String hostName;

    public RoomResponse(Long roomId, String title, String destination,
                        String meetingTime, String inviteCode, int memberCount) {
        this(roomId, title, destination, meetingTime, inviteCode, memberCount, "");
    }

    public RoomResponse(Long roomId, String title, String destination,
                        String meetingTime, String inviteCode, int memberCount, String hostName) {
        this.roomId = roomId;
        this.title = title;
        this.destination = destination;
        this.meetingTime = meetingTime;
        this.inviteCode = inviteCode;
        this.memberCount = memberCount;
        this.hostName = hostName;
    }

    public Long getRoomId() { return roomId; }
    public Long getId() { return roomId; }             // Android 호환
    public String getTitle() { return title; }
    public String getName() { return title; }          // Android 호환
    public String getDestination() { return destination; }
    public String getMeetingTime() { return meetingTime; }
    public String getAppointmentTime() { return meetingTime; } // Android 호환
    public String getInviteCode() { return inviteCode; }
    public String getCode() { return inviteCode; }     // Android 호환
    public int getMemberCount() { return memberCount; }
    public String getHostName() { return hostName; }
}