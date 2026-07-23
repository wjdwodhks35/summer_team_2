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

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}