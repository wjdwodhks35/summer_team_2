package com.example.todaydeparture.service;

import com.example.todaydeparture.dto.*;
import com.example.todaydeparture.mapper.RoomMapper;
import com.example.todaydeparture.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    private final RoomMapper roomMapper;
    private final UserMapper userMapper;

    public RoomService(RoomMapper roomMapper, UserMapper userMapper) {
        this.roomMapper = roomMapper;
        this.userMapper = userMapper;
    }

    public RoomResponse createRoom(CreateRoomRequest req, String token) {
        UserDto user = userMapper.findByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
        }

        RoomInsertParam param = new RoomInsertParam();
        param.setHostUserId(user.getId());
        param.setTitle(req.getTitle());
        param.setDestinationAddress(
            (req.getDestination() == null || req.getDestination().isEmpty()) ? "미정" : req.getDestination()
        );
        param.setMeetingDatetime(req.getMeetingTime());

        roomMapper.insertRoomParam(param);  // param.getId()에 생성된 roomId 들어옴
        Long roomId = param.getId();

        String inviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        roomMapper.insertInviteCode(roomId, inviteCode);

        int memberCount = roomMapper.countMembers(roomId);
        return new RoomResponse(roomId, req.getTitle(), req.getDestination(),
                req.getMeetingTime(), inviteCode, memberCount);
    }

    public RoomResponse joinRoom(JoinRoomRequest req, String token) {
        Long roomId = roomMapper.findRoomIdByInviteCode(req.getInviteCode());
        if (roomId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지 않은 초대코드입니다");
        }

        Long userId = null;
        String nickname = "게스트";
        String guestTokenHash = req.getGuestToken();

        if (req.getGuestToken() == null && token != null) {
            UserDto user = userMapper.findByToken(token);
            if (user != null) {
                userId = user.getId();
                nickname = user.getNickname();
                guestTokenHash = null;
            }
        }

        roomMapper.insertMember(roomId, userId, guestTokenHash, nickname);

        RoomInfoDto info = roomMapper.findRoomById(roomId);
        int memberCount = roomMapper.countMembers(roomId);
        return new RoomResponse(info.getRoomId(), info.getTitle(), info.getDestination(),
                info.getMeetingTime(), info.getInviteCode(), memberCount);
    }

    public List<RoomMemberDto> getMembers(Long roomId) {
        return roomMapper.findMembersByRoomId(roomId);
    }
}