package com.example.todaydeparture.dto;

import java.util.List;

public class RoomFullResponse {
    private Long id;
    private String name;
    private String code;
    private String destination;
    private Double destLat;
    private Double destLon;
    private String appointmentTime;
    private String hostName;
    private boolean host;
    private int memberCount;
    private List<RoomMemberFullDto> members;

    // MyBatis setters
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCode(String code) { this.code = code; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setDestLat(Double destLat) { this.destLat = destLat; }
    public void setDestLon(Double destLon) { this.destLon = destLon; }
    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }
    public void setHostName(String hostName) { this.hostName = hostName; }
    public void setHost(boolean host) { this.host = host; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    public void setMembers(List<RoomMemberFullDto> members) { this.members = members; }

    // getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getDestination() { return destination; }
    public Double getDestLat() { return destLat; }
    public Double getDestLon() { return destLon; }
    public String getAppointmentTime() { return appointmentTime; }
    public String getHostName() { return hostName; }
    public boolean isHost() { return host; }
    public int getMemberCount() { return memberCount; }
    public List<RoomMemberFullDto> getMembers() { return members; }
}
