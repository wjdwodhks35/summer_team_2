package com.example.todaydeparture.controller;

import com.example.todaydeparture.dto.*;
import com.example.todaydeparture.service.RoomService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public RoomResponse createRoom(
            @RequestBody CreateRoomRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        return roomService.createRoom(request, token);
    }

    @PostMapping("/join")
    public RoomResponse joinRoom(
            @RequestBody JoinRoomRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        return roomService.joinRoom(request, token);
    }

    @GetMapping("/{roomId}/members")
    public List<RoomMemberDto> getMembers(@PathVariable Long roomId) {
        return roomService.getMembers(roomId);
    }

    // 방 전체 정보 조회 (Android RoomDashboard 폴링용)
    @GetMapping("/{roomId}")
    public RoomFullResponse getRoomInfo(@PathVariable Long roomId) {
        return roomService.getRoomInfo(roomId);
    }

    // 내 출발지 업로드
    @PutMapping("/{roomId}/location")
    public void updateLocation(
            @PathVariable Long roomId,
            @RequestBody LocationUpdateServerRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        roomService.updateLocation(roomId, token, request);
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}